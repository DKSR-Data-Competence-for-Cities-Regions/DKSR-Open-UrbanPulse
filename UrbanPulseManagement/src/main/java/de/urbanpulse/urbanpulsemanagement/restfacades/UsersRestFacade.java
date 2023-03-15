package de.urbanpulse.urbanpulsemanagement.restfacades;

import city.ui.shared.commons.collections.Tuple;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.UserWithIds;
import de.urbanpulse.urbanpulsemanagement.services.UsersRestService;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.helper.ShiroSubjectHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.USER;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;

/**
 * REST Web Service for user management
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("users")
@Api(tags = "user")
public class UsersRestFacade extends AbstractRestFacade {

    @EJB
    private UsersRestService service;

    private ShiroSubjectHelper shiroSubjectHelper;

    public UsersRestFacade() {
        this.shiroSubjectHelper = new ShiroSubjectHelper();
    }

    public UsersRestFacade(UsersRestService service) {
        this.service = service;
        this.shiroSubjectHelper = new ShiroSubjectHelper();
    }

    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve all users.",
            response = UserTO.class,
            responseContainer = "List"
    )
    @RequiresRoles(ADMIN)
    public Response getUsers() {
        return service.getUsers();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Retrieve the user with the given ID.",
            response = UserTO.class
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response getUser(@PathParam("id") String id) {
        return service.getUser(id);
    }

    @POST
    @Consumes("application/json" + "; charset=utf-8")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Create a new user."
    )
    @RequiresRoles(ADMIN)
    public Response createUser(@ApiParam(required = true) UserWithIds user) {
        if (user == null) {
            throw new WebApplicationException("Request body must contain a user object", Response.Status.BAD_REQUEST);
        }
        UserTO userTO = mapUserTOFrom(user);
        return service.createUser(userTO, context, this);
    }

    @POST
    @Path("/{id}/resetKey")
    @Consumes("application/json" + "; charset=utf-8")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Reset the token of your own user."
    )
    @RequiresRoles(ADMIN)
    public Response resetKey(@PathParam("id") String id) {
        return service.resetKey(id, securityContext);
    }

    private UserTO mapUserTOFrom(UserWithIds user) {
        Tuple<List<RoleTO>, List<PermissionTO>> rolePermissionTuple = idsToTOs(user);
        return new UserTO(user.getName(), user.getPassword(), null, rolePermissionTuple.getFirst(), rolePermissionTuple.getSecond());

    }

    private Tuple<List<RoleTO>, List<PermissionTO>> idsToTOs(UserWithIds user) {
        List<String> roleIds = user.getRoles();
        List<RoleTO> roleTOs = roleIds == null ? EMPTY_LIST : roleIds.stream()
                .map(RoleTO::new)
                .collect(toList());
        List<String> permissionIDs = user.getPermissions();
        List<PermissionTO> permissionTOs = permissionIDs == null ? EMPTY_LIST : permissionIDs.stream()
                .map(permissionID -> new PermissionTO(permissionID, null))
                .collect(toList());
        return new Tuple<>(roleTOs, permissionTOs);
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Update the user with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response update(@PathParam("id") String id, @ApiParam(required = true) UserWithIds user) {
        if (user == null) {
            throw new WebApplicationException("Request body must contain a user object", Response.Status.BAD_REQUEST);
        }
        return service.updateUser(id, mapUserTOFrom(user), securityContext);
    }

    @PUT
    @Path("/{id}/password")
    @Consumes("text/plain")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Change the password of the user with the given ID."
    )
    @RequiresAuthentication
    public Response changePassword(@PathParam("id") String id, @ApiParam(required = true) String password) {
        if (password == null) {
            throw new WebApplicationException("Request body must contain a password", Response.Status.BAD_REQUEST);
        }
        return service.changePassword(id, password, securityContext);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(
            value = "Delete the user with the given ID."
    )
    @RequiresRoles(ADMIN)
    public Response deleteUser(@PathParam("id") String id) {
        return service.deleteUser(id);
    }

    @POST
    @Path("/{id}/permissions/sensors/{SID}")
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "Assign a permission (if not exists) to a user to access the given sensor data"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response addPermission(@PathParam("id") String id, @PathParam("SID") String sid, @ApiParam(required = true) ScopesWithOperations permissions) {
        Subject currentUser = shiroSubjectHelper.getSubject();
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
            value = "Get all the permission which linked to the user and contains the SID"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response getPermission(@PathParam("id") String id, @PathParam("SID") String sid) {
        Subject currentUser = shiroSubjectHelper.getSubject();
        if (currentUser.isPermitted("sensor:" + sid + ":permission:read")) {
            return service.getPermission(id, sid);
        } else {
            return ErrorResponseFactory.forbidden("The user doesn't have the permission: sensor:" + sid + ":permission:read");
        }
    }


    @DELETE
    @Path("/{id}/permissions/{permissionId}")
    @ApiOperation(
            value = "Delete the given permission link from the user's permission list"
    )
    @RequiresRoles(value = {ADMIN, USER}, logical = Logical.OR)
    public Response deletePermission(@PathParam("id") String id, @PathParam("permissionId") String permissionId) {
        Subject currentUser = shiroSubjectHelper.getSubject();
        if (currentUser.isPermitted("sensor:*:permission:delete")) {
            return service.deletePermission(id, permissionId);
        } else {
            return ErrorResponseFactory.forbidden("The user doesn't have the permission: sensor:*:permission:delete");
        }
    }


}
