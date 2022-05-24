package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import de.urbanpulse.urbanpulsemanagement.services.PermissionsRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresRoles;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

@Path("permissions")
@Api(tags = "permissions")
public class PermissionsRestFacade extends AbstractRestFacade {

    @EJB
    private PermissionsRestService service;

    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve all permissions.",
            response = PermissionTO.class,
            responseContainer = "List"
    )
    @RequiresRoles(ADMIN)
    public Response getPermissions() {
        return service.getPermissions();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve the user with the given ID.",
            response = PermissionTO.class
    )
    @RequiresRoles(ADMIN)
    public Response getPermission(@PathParam("id") String id) {
        return service.getPermission(id);
    }

    @POST
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Create a new permission."
    )
    @RequiresRoles(ADMIN)
    public Response createPermission(@ApiParam(required = true) PermissionTO permission) {
        return service.createPermission(permission, context, this);
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @ApiOperation(
            value = "Update the permission with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response updatePermission(@PathParam("id") String id, @ApiParam(required = true) PermissionTO permission) {
        return service.updatePermission(id,permission);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(
            value = "Delete the user with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response deletePermission(@PathParam("id") String id) {
        return service.deletePermission(id);
    }

}
