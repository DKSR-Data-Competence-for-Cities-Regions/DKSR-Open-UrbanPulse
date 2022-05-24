package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import java.util.UUID;
import javax.ejb.Stateless;

/**
 * provides access to UP vert.x module registrations in the database
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class UPModuleDAO extends AbstractStringIdDAO<UPModuleEntity> {

    public UPModuleDAO() {
        super(UPModuleEntity.class);
    }

    public UPModuleEntity create(UPModuleType moduleType) {
        return create(moduleType.name());
    }

    public UPModuleEntity create(String moduleType) {
        UPModuleEntity entity = new UPModuleEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setModuleType(moduleType);
        entityManager.persist(entity);
        return entity;
    }
}
