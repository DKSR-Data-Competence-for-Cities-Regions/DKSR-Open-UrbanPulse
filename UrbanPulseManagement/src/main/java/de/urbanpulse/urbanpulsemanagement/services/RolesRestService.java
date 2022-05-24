package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;

import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@LocalBean
@Stateless
public class RolesRestService extends AbstractRestService {

    @EJB
    private RoleManagementDAO dao;

    @EJB
    private PermissionManagementDAO permissionDao;

    public Response getRoles() {
        List<RoleTO> roles = dao.getAll();
        if (roles == null) {
            roles = new ArrayList<>();
        }
        return Response.ok(roles).build();
    }

    public Response getRole(String id) {
        RoleTO role = dao.getById(id);
        if (role != null) {
            return Response.ok(role).build();
        } else {
            return ErrorResponseFactory.notFound("role with id [" + id + "] does not exist");
        }
    }

    public Response createRole(RoleTO role, UriInfo context, AbstractRestFacade facade) {
        if (role.getId() != null && dao.getById(role.getId()) != null) {
            return ErrorResponseFactory.conflict("role with this id already exists");
        }

        List<PermissionTO> permissions = role.getPermissions();
        if (permissions != null) {
            for (PermissionTO permissionTO : permissions) {
                PermissionTO storedPermission = permissionDao.getById(permissionTO.getId());
                if (storedPermission == null) {
                    return ErrorResponseFactory.unprocessibleEntity("permission with id " + permissionTO.getId() + " not found");
                }
            }
        }

        RoleTO result = dao.createRole(role);
        if (result != null) {
            URI location = getItemUri(context, facade, result.getId());
            return Response.created(location).build();
        } else {
            return Response.serverError().build();
        }
    }

    public Response updateRole(String id, RoleTO role) {
        // Inject ID as it is not (or at least may not be) referenced in the request's body, else check
        if (role.getId() == null) {
            role.setId(id);
        } else if (!role.getId().equals(id)) {
            return ErrorResponseFactory.unprocessibleEntity("given permission id does not match path parameter");
        }

        role.setId(id);
        RoleTO result = dao.updateRole(role);
        if (result != null) {
            return Response.noContent().build();
        } else {
            return ErrorResponseFactory.notFound("role with id [" + id + "] does not exist");
        }
    }

    public Response deleteRole(String id) {
        dao.deleteById(id);
        return Response.noContent().build();
    }

    /**
     * Update User's permission list if the permission exists and the it hasn't been linked to the user
     * so that users_permissions table will be updated
     *
     * @param roleId      is the id of the role which role's permission list will be modified
     * @param sensorId    the sensor id what the permission name going to contain
     * @param permissions it contains the scopes and operations which are required to update the permission list of the user
     * @return with a 201 response if the transaction succeeded
     * or if it's not a valid body then it returns with a 400.
     */
    public Response addPermission(String roleId, String sensorId, ScopesWithOperations permissions) {
        if (isValidPermissionBodyForSensor(permissions)) {
            List<String> operations = permissions.getOperation();
            List<String> scopes = permissions.getScope();
            List<String> wildCardStrings = createWildCardStrings(sensorId, operations, scopes);
            RoleTO roleTO = dao.getById(roleId);
            if (roleTO != null) {
                for (String wildCardString : wildCardStrings) {
                    Optional<PermissionTO> currentPermission = getPermissionBasedOnName(wildCardString, permissionDao);
                    if (currentPermission.isPresent() && !roleHasThePermission(roleTO, currentPermission)) {
                        List<PermissionTO> newPermissionList = roleTO.getPermissions();
                        newPermissionList.add(currentPermission.get());
                        roleTO.setPermissions(newPermissionList);
                        dao.updateRole(roleTO);
                    } else {
                        return ErrorResponseFactory.badRequest("The permission: " + wildCardString + " + does not exists or it has been already linked to the role:" + roleId);
                    }
                }
            } else {
                return ErrorResponseFactory.badRequest("The role not exists:" + roleId);
            }
            return Response.status(Response.Status.CREATED).build();
        } else {
            return ErrorResponseFactory.badRequest("Body must only contains operation and scope arrays");
        }
    }

    /**
     * Get the role's permissions which contains the SID
     *
     * @param roleId   is the id of the role which role's permission are we interested in
     * @param sensorId is the sensor id what should be the part of the filtered permission strings
     * @return a response which body contains the filtered permission strings or empty
     */
    public Response getPermission(String roleId, String sensorId) {
        List<PermissionTO> filteredPermissions = permissionDao.getRolePermissionsBySensorId(roleId, sensorId);
        return Response.ok(filteredPermissions).build();
    }

    /**
     * Delete the permission link from the role
     *
     * @param roleId       is the id of the role which role's permission are we interested in
     * @param permissionId is the id of the permission what we want to delete
     * @return a response about the operation it can be ok if everything went well and the link has been removed
     * and if the required permission is not linked to the role then we are returning with a bad request
     */
    public Response deletePermission(String roleId, String permissionId) {
        RoleTO roleTO = dao.getById(roleId);
        List<PermissionTO> roleAllPermission = roleTO.getPermissions();
        boolean isPermissionLinkedToTheRole = roleAllPermission.stream()
                .anyMatch(permissionTO -> permissionTO.getId().equals(permissionId));
        if (isPermissionLinkedToTheRole) {
            List<PermissionTO> filteredPermissionList = roleAllPermission.stream()
                    .filter(permissionTO -> !permissionTO.getId().equals(permissionId))
                    .collect(Collectors.toList());
            roleTO.setPermissions(filteredPermissionList);
            dao.updateRole(roleTO);
            return Response.ok().build();
        } else {
            return ErrorResponseFactory.badRequest("The given permission with permission ID: " + permissionId + " is not linked to the role");
        }
    }

    private boolean roleHasThePermission(RoleTO roleTO, Optional<PermissionTO> permissionTO) {
        List<PermissionTO> permissions = roleTO.getPermissions();
        return permissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionTO.get().getName()));
    }

}
