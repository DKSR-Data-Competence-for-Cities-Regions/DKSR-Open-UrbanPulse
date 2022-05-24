package de.urbanpulse.urbanpulsemanagement.services;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.transfer.StatementsWrapperTO;
import de.urbanpulse.urbanpulsemanagement.transfer.UpdateListenersWrapperTO;
import io.vertx.core.json.JsonObject;

/**
 * REST web service to configure event processor statements
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class StatementsRestService extends AbstractRestService {

    @Inject
    EventProcessorWrapper eventProcessor;

    @Inject
    private OutboundInterfacesManagementDAO outboundInterfacesDao;

    @Inject
    private StatementManagementDAO statementDao;

    /**
     * @param id of the statement to get the update listener for.
     * @return wrapped JSON array of update listeners for this statement
     */
    public Response getUpdateListeners(String id) {
        StatementTO statement = statementDao.getById(id);
        if (statement == null) {
            return ErrorResponseFactory.notFound("statement with ID[" + id + "] not found");
        }

        List<UpdateListenerTO> listeners = outboundInterfacesDao.getUpdateListenersOfStatement(id);
        UpdateListenersWrapperTO wrapper = new UpdateListenersWrapperTO(listeners);
        censorPasswordsInUpdateListeners(wrapper);
        return Response.ok(wrapper).build();
    }

    public Response getStatements() {
        List<StatementTO> statements = statementDao.getAll();
        StatementsWrapperTO wrapper = new StatementsWrapperTO(statements);
        return Response.ok(wrapper).build();
    }

    public Response getStatement(String id) {
        StatementTO statement = statementDao.getById(id);
        if (statement == null) {
            return ErrorResponseFactory.notFound("statement with ID[" + id + "] not found");
        }

        return Response.ok(statement).build();
    }

    public Response createStatement(StatementTO statement, UriInfo context, AbstractRestFacade facade) {
        if (statement.getName() != null && statement.getQuery() != null) {
            final String name = statement.getName();
            final String query = statement.getQuery();
            final String comment = statement.getComment();

            StatementTO createdStatement = statementDao.createStatement(name, query, comment);
            if (createdStatement == null) {
                return ErrorResponseFactory.conflict("statement with name [" + name + "] already exists");
            }

            try {
                eventProcessor.registerStatement(query, name);
            } catch (EventProcessorWrapperException ex) {
                String error = ex.getErrorList().stream()
                        .map(j -> j.getJsonObject("body", new JsonObject()))
                        .filter(j -> j.containsKey("error")).map(j -> j.getString("error"))
                        .collect(Collectors.joining("\n"));

                Logger.getLogger(StatementsRestService.class.getName()).warning(error);
                return ErrorResponseFactory.internalServerError(ex.getMessage() + ": " + error);
            }

            final URI location = getItemUri(context, facade, createdStatement.getId());
            return Response.created(location).build();
        } else {
            return ErrorResponseFactory.unprocessibleEntity("incomplete json, requires fields 'name' and 'query'");
        }
    }

    public Response deleteStatement(final String id) {
        boolean hasListeners = !outboundInterfacesDao.getUpdateListenersOfStatement(id).isEmpty();
        if (hasListeners) {
            return ErrorResponseFactory.conflict("statement with id[" + id + "] still has listeners, cannot delete");
        }

        StatementTO statement = statementDao.getById(id);
        if (statement == null) {
            return Response.noContent().build();
        }

        String name = statement.getName();
        try {
            eventProcessor.unregisterStatement(name);
        } catch (EventProcessorWrapperException ex) {
            return ErrorResponseFactory.internalServerErrorFromException(ex);
        }
        statementDao.deleteById(id);
        return Response.noContent().build();
    }

    public Response registerUpdateListener(final String statementId, UpdateListenerTO updateListener, UriInfo context, Class facadeClass) {
        final AuthJsonTO authJson = updateListener.getAuthJson();
        final String targetUrl = updateListener.getTarget();

        if (targetUrl == null) {
            return ErrorResponseFactory.unprocessibleEntity("incomplete json, requires 'target'");
        }

        if(!outboundInterfacesDao.isValidAuthMethod(authJson)) {
             return ErrorResponseFactory.unprocessibleEntity("unsupported authorization method");
        }

        List<UpdateListenerTO> listenersForStatement = outboundInterfacesDao.getUpdateListenersOfStatement(statementId);
        boolean alreadyRegistered = checkIfAlreadyRegistered(listenersForStatement, targetUrl);
        if (alreadyRegistered) {
            return ErrorResponseFactory.conflict("already have a listener with target " + targetUrl + " for statement with id " + statementId);
        }

        StatementEntity statement = statementDao.queryById(statementId);

        if (statement == null) {
            return ErrorResponseFactory.notFound("no statement with id [" + statementId + "] found for which to add a statement");
        }

        Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                "Register Update Listener for statement with id {0} (triggered via REST call)",
                statementId);
        UpdateListenerTO listener = outboundInterfacesDao.createUpdateListener(statement, targetUrl, authJson);
        final String listenerId = listener.getId();

        URI location = getListenerUri(statementId, listenerId, context, facadeClass);

        try {
            eventProcessor.registerUpdateListener(new StatementTO(statement),listener);
        } catch (EventProcessorWrapperException ex) {
            return ErrorResponseFactory.internalServerErrorFromException(ex);
        }

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Listener added by http request");
        return Response.created(location).build();
    }

    private boolean checkIfAlreadyRegistered(List<UpdateListenerTO> listenersForStatement, final String targeturl) {
        boolean alreadyRegistered = false;
        for (UpdateListenerTO listener : listenersForStatement) {
            if (targeturl.equals(listener.getTarget())) {
                alreadyRegistered = true;
                break;
            }
        }
        return alreadyRegistered;
    }

    private URI getListenerUri(String statementId, String listenerId, UriInfo context, Class facadeClass) {
        UriBuilder builder = context.getBaseUriBuilder();
        return builder.path(facadeClass).path(statementId).path("update-listeners").path(listenerId).build();
    }

    public Response getListener(String statementId, String listenerId) {
        UpdateListenerTO listener = outboundInterfacesDao.getUpdateListenerById(listenerId);
        if (listener == null) {
            return ErrorResponseFactory.notFound("update listener with ID[" + listenerId + "] not found");
        }

        StatementTO statement = statementDao.getById(statementId);
        if (statement == null) {
            return ErrorResponseFactory.notFound("statement with ID[" + statementId + "] not found");
        }

        if (statementId.equals(listener.getStatementId())) {
            censorPasswordInUpdateListener(listener);
            return Response.ok(listener).build();
        }

        return ErrorResponseFactory.badRequest(
                "listener with ID[" + listenerId + "] does not belong to statement with ID[" + statementId + "]");
    }

    public Response removeListener(String statementId, String listenerId) {
        StatementTO statement = statementDao.getById(statementId);
        if (statement == null) {
            return ErrorResponseFactory.notFound("statement with ID[" + statementId + "] not found");
        }

        UpdateListenerTO listener = outboundInterfacesDao.getUpdateListenerById(listenerId);
        if (listener != null) {
            if (Objects.equals(statementId, listener.getStatementId())) {
                outboundInterfacesDao.deleteUpdateListener(listenerId);
                try {
                    eventProcessor.unregisterUpdateListener(listenerId, statement.getName());
                } catch (EventProcessorWrapperException ex) {
                    return ErrorResponseFactory.internalServerErrorFromException(ex);
                }
            } else {
                return ErrorResponseFactory.badRequest(
                        "listener with ID[" + listenerId + "] does not belong to statement with ID[" + statementId + "]");
            }
        }

        return Response.noContent().build();
    }

    private void censorPasswordInUpdateListener(UpdateListenerTO updateListener) {
        if (updateListener.getAuthJson() != null) {
            updateListener.getAuthJson().setPassword(null);
        }
    }

    private void censorPasswordsInUpdateListeners(UpdateListenersWrapperTO wrapper) {
        wrapper.getListeners().forEach(this::censorPasswordInUpdateListener);
    }
}
