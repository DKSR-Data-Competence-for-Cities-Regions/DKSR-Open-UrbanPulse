package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsemanagement.services.EventTypesRestService;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.*;
import static de.urbanpulse.urbanpulsemanagement.restfacades.EventTypesRestFacade.ROOT_PATH;

/**
 * REST Web Service for registering event types
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path(ROOT_PATH)
@Api(tags = "event type")
public class EventTypesRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "eventtypes";

    @EJB
    private EventTypesRestService service;

    @RequiresRoles(ADMIN)
    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "retrieve registered event type with given ID",
            response = EventTypeTO.class
    )
    public Response getEventType(@PathParam("id") String id) {
        return service.getEventType(id);
    }

    /**
     * delete an event type
     * @param id the id of the event type to be deleted
     * @return 204 NO CONTENT
     */
    @RequiresRoles(ADMIN)
    @DELETE
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "delete event type with given ID"
    )
    public Response deleteEventType(@PathParam("id") String id) {
        return service.deleteEventType(id);
    }

    /**
     * @return event types wrapped in JSON object
     */
    @RequiresRoles(ADMIN)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "retrieve all registered event types",
            response = EventTypeTO.class,
            responseContainer = "List"
    )
    public Response getAllEventTypes() {
        return service.getAllEventTypes();
    }

    /**
     * create a new event type registration
     *
     * @param eventTypeJson JSON string like this:<pre>
     *                                           {
     *                                           "name":"EventTyp9808023",
     *                                           "config":{
     *                                           "sequence":"long",
     *                                           "value":"string" | "long",
     *                                           "SID":"string",
     *                                           "timestamp":"date"
     *                                           } ,
     *                                           "description": {
     *                                           "sequence":"Sender sequence number",
     *                                           "value":"The measurement value"
     *                                           }
     *                                           }
     *                                           </pre>
     * @return 201 created response with location header set appropriately, if successful / 409 conflict response if
     * name not unique / 400 bad request if json invalid / 500 internal server error otherwise
     */
    @RequiresRoles(ADMIN)
    @POST
    @Consumes("application/json")
    @ApiOperation(
            value = "register new event type"
    )
    public Response createEventType(@ApiParam(required = true) String eventTypeJson) {
        try {
            return service.createEventType(eventTypeJson, context, this.getClass());
        } catch (WrappedWebApplicationException e) {
            throw e.getWebApplicationException();
        }
    }

    /**
     * Updates an existing event type
     *
     * @param id            the id of the event type to update
     * @param eventTypeJson JSON string with an optional name, description or event parameters to update
     * @return the updatedEventType Response
     */
    @RequiresRoles(ADMIN)
    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @ApiOperation(
            value = "update already registered event type with given ID"
    )
    public Response updateEventType(@PathParam("id") String id, @ApiParam(required = true) String eventTypeJson) {
        return service.updateEventType(id, eventTypeJson);
    }
}
