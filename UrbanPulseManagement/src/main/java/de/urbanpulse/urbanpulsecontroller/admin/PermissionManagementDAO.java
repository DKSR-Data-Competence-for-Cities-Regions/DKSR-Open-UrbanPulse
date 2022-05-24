package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import java.util.ArrayList;
import java.util.List;

import static javax.ejb.TransactionAttributeType.MANDATORY;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class PermissionManagementDAO extends AbstractUUIDDAO<PermissionEntity, PermissionTO> {

    public PermissionManagementDAO() {
        super(PermissionEntity.class, PermissionTO.class);
    }

    public PermissionTO createPermission(PermissionTO permission) {
        boolean nameTaken = !getFilteredBy("name", permission.getName()).isEmpty();
        if (nameTaken) {
            return null;
        }
        PermissionEntity entity = new PermissionEntity();
        entity.setName(permission.getName());
        entityManager.persist(entity);
        return toTransferObject(entity);
    }

    public PermissionTO updatePermission(PermissionTO permission) {
        PermissionEntity permissionEntity = queryById(permission.getId());
        if (permission.getName() != null && !permission.getName().isEmpty()) {
            permissionEntity.setName(permission.getName());
            return toTransferObject(permissionEntity);
        } else {
            return null;
        }
    }

    public List<PermissionTO> getRolePermissionsBySensorId(String roleId, String sensorId) {
        List<PermissionEntity> permissionEntities = entityManager.createNamedQuery("getRolePermissionsBySensorId", PermissionEntity.class)
                .setParameter("roleId", roleId)
                .setParameter("sensorId", sensorId)
                .getResultList();
        List<PermissionTO> permissionTOList = new ArrayList<>();
        permissionEntities.forEach(permissionEntity -> permissionTOList.add(toTransferObject(permissionEntity)));
        return permissionTOList;
    }

    public List<PermissionTO> getUserPermissionsBySensorId(String userId, String sensorId) {
        List<PermissionEntity> permissionEntities = entityManager.createNamedQuery("getUserPermissionsBySensorId", PermissionEntity.class)
                .setParameter("userId", userId)
                .setParameter("sensorId", sensorId)
                .getResultList();
        List<PermissionTO> permissionTOList = new ArrayList<>();
        permissionEntities.forEach(permissionEntity -> permissionTOList.add(toTransferObject(permissionEntity)));
        return permissionTOList;
    }

    @Override
    protected PermissionTO toTransferObject(PermissionEntity entity) {
        return new PermissionTO(entity);
    }


}
