package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.Hasher;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.helper.ShiroSubjectHelper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.PersistenceV3Wrapper;
import de.urbanpulse.urbanpulsemanagement.util.PasswordPolicy;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST Web Service for user management
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class UsersRestService extends AbstractRestService {

    static final String ROOT_PATH = "users";

    @Inject
    private UserManagementDAO dao;

    @Inject
    private RoleManagementDAO roleDao;

    @Inject
    private PermissionManagementDAO permissionDao;

    @Inject
    private PasswordPolicy passwordPolicy;

    @Inject
    private PersistenceV3Wrapper persistenceV3Wrapper;

    @Inject
    private ShiroSubjectHelper subjectHelper;

    @XmlRootElement
    public static class UsersWrapperTO {

        public List<UserTO> users;

        public UsersWrapperTO() {
        }

        public UsersWrapperTO(List<UserTO> users) {
            this.users = users;
        }
    }

    public Response getUsers() {
        List<UserTO> users = dao.getAll();
        UsersWrapperTO wrapper = new UsersWrapperTO(users);
        return Response.ok(wrapper).build();
    }

    public Response getUser(String id) {
        Subject currentUser = subjectHelper.getSubject();

        String subjectId = subjectHelper.getSubjectId();
        if (currentUser.hasRole(UPDefaultRoles.ADMIN) || currentUser.isPermitted("users:read:" + id) || subjectId.equals(id)) {
            UserTO user = dao.getById(id);
            if (user == null) {
                return userNotFound(id);
            } else {
                user.setSecretKey(null);
                return Response.ok(user).build();
            }
        } else {
            return userNotFound(id);
        }

    }

    public Response createUser(UserTO user, UriInfo context, AbstractRestFacade facade) {
        if (user.getId() != null && dao.getById(user.getId()) != null) {
            return ErrorResponseFactory.conflict("user with this id already exists");
        }

        String name = user.getName();
        if (name == null) {
            return ErrorResponseFactory.unprocessibleEntity("user name missing");
        }

        String password = user.getPassword();
        if (password == null) {
            return ErrorResponseFactory.unprocessibleEntity("user password missing");
        }

        List<RoleTO> roles = user.getRoles();
        if (roles != null) {
            for (RoleTO roleTO : roles) {
                RoleTO storedRole = roleDao.getById(roleTO.getId());
                if (storedRole == null) {
                    return ErrorResponseFactory.unprocessibleEntity("user role with id " + roleTO.getId() + " not found");
                }
            }
        }

        List<PermissionTO> permissions = user.getPermissions();
        if (permissions != null) {
            for (PermissionTO permissionTO : permissions) {
                PermissionTO storedPermission = permissionDao.getById(permissionTO.getId());
                if (storedPermission == null) {
                    return ErrorResponseFactory.unprocessibleEntity("permission with id " + permissionTO.getId() + " not found");
                }
            }
        }

        if (!passwordPolicy.isAcceptable(password)) {
            return ErrorResponseFactory.badRequest("Password does not meet policy requirements. "
                    + "Must be 8 or more characters with at least one character out of each of the following group: "
                    + passwordPolicy.PASSWORD_POLICY_HUMAN_READABLE);
        }

        setSecretKey(user);
        UserTO createdUser = dao.createUser(user);
        if (createdUser == null) {
            return ErrorResponseFactory.conflict("user name [" + name + "] already taken");
        }
        createdUser.setPassword(user.getPassword());

        URI location = getItemUri(context, facade, createdUser.getId());

        return Response.created(location).entity(createdUser).build();
    }

    private void setSecretKey(UserTO userTO) {
        String secretKey;
        try {
            secretKey = Hasher.generateRandomHmacSha256Key();
            userTO.setSecretKey(secretKey);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(UsersRestService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public Response deleteUser(String id) {
        UserTO user = dao.getById(id);
        if (user == null) {
            return Response.noContent().build();
        }

        dao.deleteById(id);

        return Response.noContent().build();
    }

    public Response changePassword(String id, String password, SecurityContext context) {
        boolean updateAllowed = context.isUserInRole(UPDefaultRoles.ADMIN) || id.equals(subjectHelper.getSubjectId());

        if (!updateAllowed) {
            return ErrorResponseFactory.forbidden("changing password of user with id[" + id + "] forbidden");
        } else if (!passwordPolicy.isAcceptable(password)) {
            return ErrorResponseFactory.badRequest("Password does not meet policy requirements. "
                    + "Must be 8 or more characters with at least one character out of each of the following group: "
                    + PasswordPolicy.PASSWORD_POLICY_HUMAN_READABLE);
        }

        final UserTO user = dao.changePassword(id, password);
        if (user == null) {
            return userNotFound(id);
        }

        return Response.noContent().build();
    }

    public Response resetKey(String id, SecurityContext securityContext) {
        UserTO persistedUser = dao.getById(id);
        if (persistedUser == null) {
            return userNotFound(id);
        }

        LoginToken token = getLoginToken();
        if (token == null) {
            return ErrorResponseFactory.fromStatus(Response.Status.UNAUTHORIZED, "Principal was not of type LoginToken");
        }

        if (token.getAuthmode() != UPAuthMode.BASIC) {
            return ErrorResponseFactory.fromStatus(Response.Status.FORBIDDEN, "Not allowed to reset token with token login");
        }

        if (!token.getSubjectId().equals(id)) {
            String message = "not allowed to update user with id [" + id + "]";
            return ErrorResponseFactory.fromStatus(Response.Status.FORBIDDEN, message);
        }

        setSecretKey(persistedUser);
        UserTO updatedUser = dao.update(persistedUser, true);
        updatedUser.setSecretKey(persistedUser.getSecretKey());

        return Response.ok(updatedUser).build();
    }

    protected LoginToken getLoginToken() {
        if (SecurityUtils.getSubject().getPrincipal() instanceof LoginToken) {
            return ((LoginToken) SecurityUtils.getSubject().getPrincipal());
        } else {
            return null;
        }
    }

    public Response updateUser(String id, UserTO user, SecurityContext securityContext) {
        // Inject ID as it is not (or at least may not be) referenced in the request's body, else check
        if (user.getId() == null) {
            user.setId(id);
        } else if (!user.getId().equals(id)) {
            return ErrorResponseFactory.unprocessibleEntity("given user id does not match path parameter");
        }

        UserTO persistedUser = dao.getById(id);
        if (persistedUser == null) {
            return userNotFound(id);
        }

        final boolean isAdmin = securityContext.isUserInRole(UPDefaultRoles.ADMIN);
        final boolean allRolesExist = user
                .getRoles()
                .stream()
                .map(RoleTO::getId)
                .map(roleDao::getById)
                .allMatch(Objects::nonNull);
        final boolean allPermissionsExist = user
                .getPermissions()
                .stream()
                .map(PermissionTO::getId)
                .map(permissionDao::getById)
                .allMatch(Objects::nonNull);

        if (!allRolesExist || !allPermissionsExist) {
            String message = "could not update user with id [" + id + "]";
            if (!allRolesExist) {
                message += "; not all roles exist";
            }
            if (!allPermissionsExist) {
                message += "; not all permissions exist";
            }
            return ErrorResponseFactory.unprocessibleEntity(message);
        }
        if (!isAdmin) {
            String message = "not allowed to update user with id [" + id + "]";
            return ErrorResponseFactory.conflict(message);
        }

        String password = user.getPassword();
        if (password != null && !passwordPolicy.isAcceptable(password)) {
            return ErrorResponseFactory.badRequest("Password does not meet policy requirements. "
                    + "Must be 8 or more characters with at least one character out of each of the following group: "
                    + passwordPolicy.PASSWORD_POLICY_HUMAN_READABLE);
        }

        dao.update(user);

        return Response.ok(user).build();
    }

    private static Response userNotFound(String id) {
        return ErrorResponseFactory.notFound("user with id[" + id + "] not found");
    }

    /**
     * Update User's permission list if the permission exists and the it hasn't been linked to the user
     * so that users_permissions table will be updated
     *
     * @param userId      the id of the user whose permissions will be modified
     * @param sensorId    the sensor id what the permission name going to contain
     * @param permissions it contains the scopes and operations which are required to update the permission list of the user
     * @return with a 201 response if the transaction succeeded
     * or if it's not a valid body then it returns with a 400.
     */
    public Response addPermission(String userId, String sensorId, ScopesWithOperations permissions) {
        if (isValidPermissionBodyForSensor(permissions)) {
            List<String> operations = permissions.getOperation();
            List<String> scopes = permissions.getScope();
            List<String> wildCardStrings = createWildCardStrings(sensorId, operations, scopes);
            UserTO userTO = dao.getById(userId);
            if(userTO != null) {
                for (String wildCardString : wildCardStrings) {
                    Optional<PermissionTO> currentPermission = getPermissionBasedOnName(wildCardString, permissionDao);
                    if (currentPermission.isPresent() && !isPermissionAssignedToUser(userTO, currentPermission.get())) {
                        List<PermissionTO> newPermissionList = userTO.getPermissions();
                        newPermissionList.add(currentPermission.get());
                        userTO.setPermissions(newPermissionList);
                        dao.update(userTO);
                    } else {
                        return ErrorResponseFactory.badRequest("The permission: " + wildCardString + " + does not exists or it has been already linked to the user:" + userId);
                    }
                }
            }else{
                return ErrorResponseFactory.badRequest("The user: "+ userId +" is not existing");
            }
            return Response.status(Response.Status.CREATED).build();
        } else {
            return ErrorResponseFactory.badRequest("Body must only contains operation and scope arrays");
        }
    }

    private boolean isPermissionAssignedToUser(UserTO userTO, PermissionTO permissionTO) {
        return userTO.getPermissions().stream()
                .anyMatch(permission -> permission.getName().equals(permissionTO.getName()));
    }

    /**
     * Get the user's permissions which contains the SID
     *
     * @param userId   is the id of the user whose permission are we interested in
     * @param sensorId is the sensor id what should be the part of the filtered permission strings
     * @return a response which body contains the filtered permission strings or empty
     */
    public Response getPermission(String userId, String sensorId) {
        List<PermissionTO> filteredPermissions = permissionDao.getUserPermissionsBySensorId(userId, sensorId);
        return Response.ok(filteredPermissions).build();
    }

    /**
     * Delete the permission link from the user
     *
     * @param userId       is the id of the user whose permission are we interested in
     * @param permissionId is the id of the permission what we want to delete
     * @return a response about the operation it can be ok if everything went well and the link has been removed
     * and if the required permission is not linked to the user then we are returning with a bad request
     */
    public Response deletePermission(String userId, String permissionId) {
        UserTO userTO = dao.getById(userId);
        List<PermissionTO> userAllPermission = userTO.getPermissions();
        boolean isPermissionLinkedToTheUser = userAllPermission.stream()
                .anyMatch(permissionTO -> permissionTO.getId().equals(permissionId));
        if (isPermissionLinkedToTheUser) {
            List<PermissionTO> filteredPermissionList = userAllPermission.stream()
                    .filter(permissionTO -> !permissionTO.getId().equals(permissionId))
                    .collect(Collectors.toList());
            userTO.setPermissions(filteredPermissionList);
            dao.update(userTO);
            return Response.ok().build();
        } else {
            return ErrorResponseFactory.badRequest("The given permission with permission ID: " + permissionId + " is not linked to the user");
        }
    }
}
