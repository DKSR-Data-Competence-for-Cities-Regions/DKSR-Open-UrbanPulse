package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;

import de.urbanpulse.urbanpulsemanagement.restfacades.dto.RoleWithIds;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.RolesRestService;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.USER;
import static java.util.Collections.EMPTY_LIST;

import java.util.List;

import static java.util.stream.Collectors.toList;

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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("roles")
@Api(tags = "roles")
public class RolesRestFacade extends AbstractRestFacade {

    @EJB
    private RolesRestService service;

    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve all roles.",
            response = RoleTO.class,
            responseContainer = "List"
    )
    @RequiresRoles(ADMIN)
    public Response getRoles() {
        return service.getRoles();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve the user with the given ID.",
            response = RoleTO.class
    )
    @RequiresRoles(ADMIN)
    public Response getRole(@PathParam("id") String id) {
        return service.getRole(id);
    }

    @POST
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Create a new role."
    )
    @RequiresRoles(ADMIN)
    public Response createRole(@ApiParam(required = true) RoleWithIds role) {
        return service.createRole(new RoleTO(null, role.getName(), roleIDsToRoleTOs(role.getPermissions())), context, this);
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @ApiOperation(
            value = "Update the role with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response updateRole(@PathParam("id") String id, @ApiParam(required = true) RoleWithIds role) {
        return service.updateRole(id, new RoleTO(id, role.getName(), roleIDsToRoleTOs(role.getPermissions())));
    }

    private List<PermissionTO> roleIDsToRoleTOs(List<String> roleIDs) {
        return roleIDs == null ? EMPTY_LIST : roleIDs.stream()
                .map(id -> new PermissionTO(id, null))
                .collect(toList());
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(
            value = "Delete the user with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response deleteRole(@PathParam("id") String id) {
        return service.deleteRole(id);
    }

    @POST
    @Path("/{id}/permissions/sensors/{SID}")
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Link a permission (if not exists) to a role"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response addPermission(@PathParam("id") String id, @PathParam("SID") String sid, @ApiParam(required = true) ScopesWithOperations permissions) {
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isPermitted("sensor:" + sid + ":permission:write")) {
            return service.addPermission(id, sid, permissions);
        } else {
            return ErrorResponseFactory.forbidden("The user doesn't have the permission: sensor:" + sid + ":permission:write");
        }
    }

    @GET
    @Path("/{id}/permissions/sensors/{SID}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Get all the permission which linked to the role and contains the SID"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response getPermission(@PathParam("id") String id, @PathParam("SID") String sid) {
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isPermitted("sensor:" + sid + ":permission:read")) {
            return service.getPermission(id, sid);
        } else {
            return ErrorResponseFactory.forbidden("The user doesn't have the permission: sensor:" + sid + ":permission:read");
        }
    }


    @DELETE
    @Path("/{id}/permissions/{permissionId}")
    @ApiOperation(
            value = "Delete the given permission link from the role's permission list"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response deletePermission(@PathParam("id") String id, @PathParam("permissionId") String permissionId) {
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isPermitted("sensor:*:permission:delete")) {
            return service.deletePermission(id, permissionId);
        } else {
            return ErrorResponseFactory.forbidden("The user doesn't have the permission: sensor:*:permission:delete");
        }
    }


}
