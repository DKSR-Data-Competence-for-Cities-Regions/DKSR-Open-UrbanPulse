package de.urbanpulse.urbanpulsemanagement.util;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.EventTypeException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.JsonObject;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class EventTypesRegistrar {

    @EJB
    private EventProcessorWrapper eventProcessor;

    @EJB
    private EventTypeManagementDAO dao;

    public String deleteEventTypeById(String id) {
        return deleteEventTypeById(id, true);
    }

    public String deleteEventTypeById(String id, boolean unregisterFromCEP) {
        EventTypeTO deleteMe = dao.getById(id);
        if (deleteMe == null) {
            return null;
        }

        String name = deleteMe.getName();
        try {
            dao.deleteById(id);
            if (unregisterFromCEP) {
                eventProcessor.unregisterEventType(name);
            }
        } catch (EventProcessorWrapperException | EventTypeException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "failed to unregister event type", ex);
            throw new EventTypeRegistrarException(ex.getMessage());
        }
        return id;
    }

    public EventTypeTO getEventTypeById(String id) {
        return dao.getById(id);
    }

    /**
     * @return all registered EventTypeEntities
     */
    public List<EventTypeTO> getAllEventTypes() {
        return dao.getAll();
    }

    /**
     * register event type in event processor and persist its meta-data
     *
     * @param jsonObject JSON object from string like this:
     *
     * <pre>
     * { "name":"EventTyp9808023", "config":{ "sequence":"long",
     * "value":"string" | "long", "SID":"string", "timestamp":"java.util.Date" } , "description": { "sequence":"Sender sequence number",
     * "value":"The measurement value" } }
     * </pre>
     *
     * @return event type
     * @throws WrappedWebApplicationException with status 500 registration or meta-data persistence failed
     */
    public EventTypeTO registerEventType(JsonObject jsonObject) throws WrappedWebApplicationException {
        JsonObject config = jsonObject.getJsonObject("config");
        JsonObject description = jsonObject.getJsonObject("description");
        final String name = jsonObject.getString("name");

        if (dao.eventTypeExists(name)) {
            String errorMessage = "Cannot create EventType with name " + name + ". An EventType with the same name already exists.";
            throw new WrappedWebApplicationException(new WebApplicationException(errorMessage, Response.status(Response.Status.CONFLICT).entity(errorMessage).build()));
        }

        try {
            eventProcessor.registerEventType(name, config);
            return persistMetadata(name, description, config);
        } catch (EventProcessorWrapperException ex) {
            throw new WrappedWebApplicationException(new WebApplicationException(ex.getMessage(),
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build()));
        }
    }

    private EventTypeTO persistMetadata(final String name, JsonObject description, JsonObject config)
            throws WrappedWebApplicationException {
        EventTypeTO created = dao.createEventType(name, description.toString(), config.toString());
        if (created == null) {
            Logger.getLogger(EventTypesRegistrar.class.getName()).log(Level.SEVERE,
                    "failed to persist metadata for event type [{0}]", name);
            throw new WrappedWebApplicationException(new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR));
        }

        return created;
    }

}
