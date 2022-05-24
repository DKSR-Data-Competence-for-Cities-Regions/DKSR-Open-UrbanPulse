package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventConfigValidator;
import de.urbanpulse.urbanpulsemanagement.transfer.EventTypesWrapperTO;
import de.urbanpulse.urbanpulsemanagement.util.EventTypeRegistrarException;
import de.urbanpulse.urbanpulsemanagement.util.EventTypesRegistrar;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service for registering event types
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class EventTypesRestService extends AbstractRestService {

    @EJB
    private EventConfigValidator eventConfigValidator;

    @EJB
    private EventTypesRegistrar registrar;

    @EJB
    private EventTypeManagementDAO dao;

    /*
    @Inject
    @ManagementUserKeyLookupMode
    private MessageAuthenticator messageAuthenticator;
     */

    public Response getEventType(String id) {
        EventTypeTO eventType = registrar.getEventTypeById(id);
        if (eventType == null) {
            return ErrorResponseFactory.notFound("event type with ID[" + id + "] not found");
        }
        return Response.ok(eventType.toJson()).build();
    }

    /**
     * delete an event type
     *
     * @param id the id of the event type requested to delete
     * @return 204 NO CONTENT
     */
    public Response deleteEventType(String id) {
        try {
        registrar.deleteEventTypeById(id);
        } catch (EventTypeRegistrarException ex) {
            return ErrorResponseFactory.conflict(ex.getMessage());
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * @return event types wrapped in JSON object
     */
    public Response getAllEventTypes() {
        List<EventTypeTO> eventTypes = new ArrayList<>(registrar.getAllEventTypes());
        EventTypesWrapperTO wrapper = new EventTypesWrapperTO(eventTypes);
        return Response.ok(wrapper.toJson()).build();
    }

    /**
     * create a new event type registration
     *
     * @param jsonString JSON string like this:<pre>
     * {
     * "name":"EventTyp9808023",
     * "config":{
     * "sequence":"long",
     * "value":"string" | "long",
     * "SID":"string",
     * "timestamp":"date"
     * } ,
     * "description": {
     * "sequence":"Sender sequence number",
     * "value":"The measurement value"
     * }
     * }
     * </pre>
     * @param context used to get base builder for the URI
     * @param facadeClass the REST facade
     *
     * @return 201 created response with location header set appropriately, if successful / 409 conflict response if
     * name not unique / 422 unproc entity if json invalid or bad config / 500 internal server error otherwise
     * @throws WrappedWebApplicationException for various error codes
     */
    public Response createEventType(String jsonString, UriInfo context, Class facadeClass) throws WrappedWebApplicationException {
        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = reader.readObject();

        if (jsonObject.containsKey("config") && jsonObject.containsKey("name") && jsonObject.containsKey("description")) {
            JsonObject config = jsonObject.getJsonObject("config");
            if (eventConfigValidator.isInvalid(config)) {
                return ErrorResponseFactory.unprocessibleEntity("invalid event type config[" + config + "]");
            }

            EventTypeTO created = registrar.registerEventType(jsonObject);
            URI uri = getItemUri(context, facadeClass, created.getId());
            return Response.created(uri).build();
        } else {
            return ErrorResponseFactory.unprocessibleEntity("invalid event type JSON");
        }
    }

    protected URI getItemUri(UriInfo uriInfo, Class serviceClass, String id) throws IllegalArgumentException, UriBuilderException {
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        return builder.path(serviceClass).path(id).build();
    }

    /**
     * Updates an existing event type
     *
     * @param id the id of the event type to update
     * @param jsonString JSON string with an optional name, description or event parameters to update
     * @return an empty response
     */
    public Response updateEventType(String id, String jsonString) {

        EventTypeTO eventToUpdate = dao.getById(id);
        if (eventToUpdate == null) {
            return ErrorResponseFactory.notFound("eventType to update with id [" + id + "] does not exist");
        }

        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        String name = null;
        if (jsonObject.containsKey("name")) {
            name = jsonObject.getString("name");
        }

        String description = null;
        if (jsonObject.containsKey("description")) {
            JsonObject descriptionObject = jsonObject.getJsonObject("description");
            description = descriptionObject.toString();
        }

        String config = null;
        if (jsonObject.containsKey("config")) {
            JsonObject configObject = jsonObject.getJsonObject("config");
            config = configObject.toString();
        }

        dao.updateEventType(id, name, description, config);
        return Response.noContent().build();
    }
}
