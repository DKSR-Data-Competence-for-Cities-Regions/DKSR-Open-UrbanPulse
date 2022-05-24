package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import org.mindrot.jbcrypt.BCrypt;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class UserManagementDAO extends AbstractUUIDDAO<UserEntity, UserTO> {

    public UserManagementDAO() {
        super(UserEntity.class, UserTO.class);
    }

    /**
     * @param userTO user data
     * @return user, null if username was already taken
     */
    public UserTO createUser(UserTO userTO) {
        boolean nameTaken = !getFilteredBy("name", userTO.getName()).isEmpty();
        if (nameTaken) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setName(userTO.getName());
        entity.setKey(userTO.getSecretKey());
        String passwordHash = BCrypt.hashpw(userTO.getPassword(), BCrypt.gensalt());
        entity.setPasswordHash(passwordHash);
        entity.setRoles(resolveRoles(userTO.getRoles()));
        entity.setPermissions(resolvePermissions(userTO.getPermissions()));
        entityManager.persist(entity);
        entityManager.flush();
        UserTO toTransferObject = toTransferObject(entity);
        // The transfer object has the secret key field set to null by default so that it's not returned in (e.g.) get requests.
        // In the create case, we explicitly want to return the secret key though, that's why it's set "manually" here
        toTransferObject.setSecretKey(userTO.getSecretKey());
        return toTransferObject;
    }

    private List<RoleEntity> resolveRoles(List<RoleTO> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        final List<RoleEntity> resolvedRoles = roles.stream().map(this::resolveRole).collect(Collectors.toList());
        if (resolvedRoles.contains(null)) {
            throw new IllegalArgumentException("not all roles could be resolved");
        }
        return resolvedRoles;
    }

    private RoleEntity resolveRole(RoleTO role) {
        return entityManager.find(RoleEntity.class, role.getId());
    }

    private List<PermissionEntity> resolvePermissions(List<PermissionTO> permissions) {
        if (permissions == null) {
            return Collections.emptyList();
        }
        return permissions.stream().map(this::resolvePermission).collect(Collectors.toList());
    }

    private PermissionEntity resolvePermission(PermissionTO permission) {
        return entityManager.find(PermissionEntity.class, permission.getId());
    }

    public UserTO update(UserTO userTO) {
        return update(userTO, false);
    }

    public UserTO update(UserTO userTO, boolean saveKey) {
        UserEntity entity = queryById(userTO.getId());
        if (entity == null) {
            return null;
        }

        if (userTO.getPassword() != null) {
            String passwordHash = BCrypt.hashpw(userTO.getPassword(), BCrypt.gensalt());
            entity.setPasswordHash(passwordHash);
        }

        if (userTO.getName() != null && !userTO.getName().equals(entity.getName())) {
            boolean nameTaken = !getFilteredBy("name", userTO.getName()).isEmpty();
            if (nameTaken) {
                return null;
            }
            entity.setName(userTO.getName());
        }

        if (saveKey) {
            entity.setKey(userTO.getSecretKey());
        }

        if (userTO.getPermissions() != null) {
            entity.setPermissions(resolvePermissions(userTO.getPermissions()));
        }

        if (userTO.getRoles() != null) {
            entity.setRoles(resolveRoles(userTO.getRoles()));
        }

        entityManager.merge(entity);
        entityManager.flush();

        return toTransferObject(entity);
    }

    public List<UserTO> getAdmins() {
        List<UserEntity> entities = queryAdmins();
        return entities.stream().map(UserTO::new).collect(Collectors.toList());
    }

    public List<UserEntity> queryAdmins() {
        final List<RoleEntity> adminRoles = entityManager
                .createNamedQuery("getRoleByName", RoleEntity.class)
                .setParameter("roleName", UPDefaultRoles.ADMIN)
                .getResultList();
        switch (adminRoles.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return adminRoles.get(0).getUsers();
            default:
                throw new IllegalStateException("There's more than one admin role");
        }
    }

    @Override
    protected UserTO toTransferObject(UserEntity entity) {
        return new UserTO(entity);
    }
}
