package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.VirtualSensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.FailedToPersistStatementException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import de.urbanpulse.urbanpulsemanagement.services.*;
import de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.*;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class VirtualSensorsCreator {

    @Inject
    private VirtualSensorConfigurator virtualSensorConfigurator;

    @Inject
    private VirtualSensorManagementDAO virtualSensorsDAO;

    @Inject
    private VirtualSensorsDependencyCreator dependencyCreator;

    /**
     * @param jsonObject The JSON that contains the configuration for creating
     *                   the virtual sensor
     * @param context    The context the request was triggered from
     * @param facade     The rest Facade the request was triggered from
     * @return a 201 http response if the virtual sensor has been created, or an
     * 400 http response if the configuration is erroneous
     * @throws WrappedWebApplicationException event type registration failed
     */
    public Response createVirtualSensor(JsonObject jsonObject, UriInfo context, AbstractRestFacade facade) {

        String categoryId;
        VirtualSensorConfiguration configuration;

        try {
            categoryId = jsonObject.getString("category");
            configuration = virtualSensorConfigurator.createVirtualSensorConfiguration(jsonObject);
        } catch (VirtualSensorConfigurationException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.toString()).build();
        }

        return createVirtualSensorFromConfig(categoryId, configuration, context, facade);
    }

    /**
     * @param virtualSensorId     the updated virtual sensor id
     * @param resultStatementName the result statement name of the virtual sensor
     * @param targets             the updated targets array of the virtual sensor
     * @param context             used to get base builder for the URI
     * @param facade              The rest facade
     * @return a response containing the virtual sensor's URI or an HTTP 422
     */
    public Response updateVirtualSensorTargets(String virtualSensorId, String resultStatementName, String targets, UriInfo context, AbstractRestFacade facade) {
        virtualSensorsDAO.updateVirtualSensorTargets(virtualSensorId, targets);
        URI virtualSensorUri = getItemUri(context, facade, virtualSensorId);
        final Response response = Response.created(virtualSensorUri).build();
        Optional<Response> errorResponse = updateVirtualSensorTargetsWithEventProcessor(resultStatementName, targets);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }
        return response;
    }

    /**
     * @param context       used to get base builder for the URI
     * @param categoryId    the id of the category
     * @param configuration the configuration of the virtual sensor
     * @param facade        the REST facade
     * @return a response containing the virtual sensor's URI or an HTTP 422
     * response containing errors raised by the CEP
     * @throws WrappedWebApplicationException event type registration failed
     */
    public Response createVirtualSensorFromConfig(String categoryId, VirtualSensorConfiguration configuration,
                                                  UriInfo context, AbstractRestFacade facade) {

        try {
            configuration.validate();
        } catch (VirtualSensorConfigurationException e) {
            return ErrorResponseFactory.badRequest(e.getMessage());
        }

        try {
            String eventTypeIdsJson = dependencyCreator.persistEventTypes(configuration.getEventTypes()).toString();
            String resultEventTypeId = dependencyCreator.persistEventType(configuration.getResultEventType()).getId();
            String resultStatementId = dependencyCreator.persistStatement(configuration.getResultstatement()).getId();
            String statementIdsJson = dependencyCreator.persistStatements(configuration.getStatments()).toString();
            List<String> targets = configuration.getTargets();

            String descriptionJson = configuration.getDescription().toString();
            VirtualSensorTO createdVirtualSensor = virtualSensorsDAO.createVirtualSensorAndReplaceSidPlaceholder(
                    categoryId, resultStatementId, statementIdsJson,
                    descriptionJson, eventTypeIdsJson, resultEventTypeId, targets);
            String virtualSensorId = createdVirtualSensor.getId();

            URI virtualSensorUri = getItemUri(context, facade, createdVirtualSensor.getSid());
            final Response response = Response.created(virtualSensorUri).build();
            Optional<Response> errorResponse = bulkRegisterWithEventProcessor(virtualSensorId, eventTypeIdsJson, resultStatementId, statementIdsJson, resultEventTypeId, targets);
            if (errorResponse.isPresent()) {
                return errorResponse.get();
            }
            return response;
        } catch (ReferencedEntityMissingException | FailedToPersistStatementException e) {
            Logger.getLogger(VirtualSensorsCreator.class.getName()).log(Level.SEVERE, null, e);
            throw getWrappedException(e);
        }
    }

    private Optional<Response> updateVirtualSensorTargetsWithEventProcessor(String resultStatementName, String targets) {
        try {
            dependencyCreator.virtualSensorTargetsUpdateWithEventProcessor(resultStatementName, targets);
        } catch (EventProcessorWrapperException ex) {
            io.vertx.core.json.JsonObject jsonErrorObj = buildErrorJsonFromErrorList(ex.getErrorList());
            return Optional.of(Response.status(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON).entity(jsonErrorObj.encodePrettily()).build());
        }
        return Optional.empty();
    }

    private Optional<Response> bulkRegisterWithEventProcessor(String virtualSensorId, String eventTypeIdsJson, String resultStatementId, String statementIdsJson, String resultEventTypeId, List<String> targets) {
        try {
            dependencyCreator.bulkRegisterWithEventProcessor(virtualSensorId, eventTypeIdsJson, resultStatementId, statementIdsJson, resultEventTypeId, targets);
        } catch (EventProcessorWrapperException ex) {
            io.vertx.core.json.JsonObject jsonErrorObj = buildErrorJsonFromErrorList(ex.getErrorList());
            return Optional.of(Response.status(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON).entity(jsonErrorObj.encodePrettily()).build());
        }
        return Optional.empty();
    }

    private RuntimeException getWrappedException(Exception e) {
        if (e instanceof ReferencedEntityMissingException || e instanceof FailedToPersistStatementException) {
            throw new WrappedWebApplicationException(new ClientErrorException(e.getMessage(), Response.Status.BAD_REQUEST));
        }

        if (e instanceof WrappedWebApplicationException) {
            throw (WrappedWebApplicationException) e;
        }

        throw new WrappedWebApplicationException(new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, e));
    }

    private URI getItemUri(UriInfo context, AbstractRestFacade facade, String id) {
        UriBuilder builder = context.getBaseUriBuilder();
        return builder.path(facade.getClass()).path(id).build();
    }

    protected io.vertx.core.json.JsonObject buildErrorJsonFromErrorList(List<io.vertx.core.json.JsonObject> ex) {
        io.vertx.core.json.JsonObject jsonObj = new io.vertx.core.json.JsonObject();

        List<String> errorStringList = ex.stream().sequential().filter(Objects::nonNull).map(error ->
                error.getJsonObject("body").getString("error", null)
        ).collect(Collectors.toList());

        return jsonObj.put("error", errorStringList);
    }
}
