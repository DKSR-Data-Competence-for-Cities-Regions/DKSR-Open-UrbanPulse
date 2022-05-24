package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import static javax.ejb.TransactionAttributeType.MANDATORY;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class RoleManagementDAO extends AbstractUUIDDAO<RoleEntity, RoleTO> {

    public RoleManagementDAO() {
        super(RoleEntity.class, RoleTO.class);
    }

    @Override
    public List<RoleTO> getAll() {
        return queryAll().stream().map(roleEntity -> {
            RoleTO to = new RoleTO(roleEntity);
            List<PermissionTO> permissions = roleEntity.getPermissions().stream().map(PermissionTO::new).collect(Collectors.toList());
            to.setPermissions(permissions);
            return to;
        }).collect(Collectors.toList());
    }

    /**
     * @param roleTO role data
     * @return role
     */
    public RoleTO createRole(RoleTO roleTO) {
        boolean nameTaken = !getFilteredBy("name", roleTO.getName()).isEmpty();
        if (nameTaken) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        entityManager.persist(entity);
        entity.setName(roleTO.getName());
        updatePermissions(entity, roleTO);

        entityManager.flush();
        return toTransferObject(entity);
    }

    private void updatePermissions(RoleEntity roleEntity, RoleTO roleTO) {
        roleTO.getPermissions().forEach(to -> {
            PermissionEntity e = entityManager.find(PermissionEntity.class, to.getId());
            roleEntity.getPermissions().add(e);
        });
    }

    public RoleTO updateRole(RoleTO roleTO) {
        RoleEntity entity = queryById(roleTO.getId());
        if (entity == null) {
            return null;
        }

        if (roleTO.getName() != null && !roleTO.getName().equals(entity.getName())) {
            boolean nameTaken = !getFilteredBy("name", roleTO.getName()).isEmpty();
            if (nameTaken) {
                return null;
            }
            entity.setName(roleTO.getName());
        }

        if (roleTO.getPermissions() != null) {
            List<PermissionEntity> list = new ArrayList<>();
            entity.setPermissions(list);
            updatePermissions(entity, roleTO);
        }

        entityManager.merge(entity);
        entityManager.flush();
        return toTransferObject(entity);
    }

    @Override
    protected RoleTO toTransferObject(RoleEntity entity) {
        return new RoleTO(entity);
    }
}
