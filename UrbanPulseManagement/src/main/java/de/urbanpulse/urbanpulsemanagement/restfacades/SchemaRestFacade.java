package de.urbanpulse.urbanpulsemanagement.restfacades;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.APP_USER;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.CONNECTOR;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.CONNECTOR_MANAGER;
import de.urbanpulse.urbanpulsemanagement.services.SchemaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "schemas")
@Path("schemas")
public class SchemaRestFacade extends AbstractRestFacade {
    
    @EJB
    SchemaService service;
    
    
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "retrieve registered schema with given ID"
    )
    public Response getEventType(@PathParam("id") String id) {
        return service.getEventTypeAsJsonSchema(id, context);
    }
    
    
    /**
     * @return all event types as Json Schema object
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "retrieve all registered schema"
    )
    public Response getAllEventTypes(@QueryParam("name") String schemaName) {
        if(schemaName != null) {
            return service.getEventTypeAsJsonSchemaByName(schemaName, context);
        }
        return service.getAllEventTypesAsJsonSchema(context);
    }

}
