package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.AbstractModuleSetupEntity;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * @param <E> the specific entity class this DAO manages
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractSetupDAO<E extends AbstractModuleSetupEntity> implements ModuleSetup {

    @PersistenceContext(unitName = "UrbanPulseManagement-PU")
    protected EntityManager entityManager;

    protected abstract Class<E> getClazz();

    public E getAndAssignConfig(String moduleId) {
        // Check if that exact module already has a config - if so, return the same config again.
        // Otherwise the config would be blocked by the module itself as it is still sending heartbeats,
        // therefore the config would never be released nor re-assigned to the module.
        E entity = getAssignedConfig(moduleId);
        if (entity != null) {
            return entity;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(root);
        criteriaQuery.where(builder.isNull(root.get("moduleId")));

        Query query = entityManager
                .createQuery(criteriaQuery)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE) // Prevents other transactions from reading while we're working on a line
                .setHint("javax.persistence.lock.timeout", 5000); // Fail after this many ms - module will request setup again
        List<E> entities = query.getResultList();
        if (entities.isEmpty()) {
            // No available config found
            return null;
        } else {
            entity = entities.get(0);
            entity.setModuleId(moduleId);
            return entityManager.merge(entity);
        }
    }

    private E getAssignedConfig(String moduleId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(root);
        criteriaQuery.where(builder.equal(root.get("moduleId"), moduleId));
        Query query = entityManager
                .createQuery(criteriaQuery)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE) // Prevents other transactions from reading while we're working on a line
                .setHint("javax.persistence.lock.timeout", 5000); // Fail after this many ms - module will request setup again
        List<E> entities = query.getResultList();
        if (entities.isEmpty()) {
            // No available config found
            return null;
        } else {
            return entities.get(0);
        }
    }

    public List<E> queryAll() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(root);
        Query query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public boolean hasConfig() {
        return countConfig() > 0;
    }

    public int countConfig() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(builder.count(root));
        Query query = entityManager.createQuery(criteriaQuery);
        return ((Long) query.getSingleResult()).intValue();
    }

    public void clearAssignments() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(root);
        criteriaQuery.where(builder.isNotNull(root.get("moduleId")));
        Query query = entityManager.createQuery(criteriaQuery);
        List<E> assignedEntities = query.getResultList();
        for (E entity : assignedEntities) {
            entity.setModuleId(null);
            entityManager.merge(entity);
        }
    }

    public void unassign(String moduleId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(getClazz());
        criteriaQuery.select(root);
        criteriaQuery.where(builder.equal(root.get("moduleId"), moduleId));
        Query query = entityManager.createQuery(criteriaQuery);
        List<E> assignedEntities = query.getResultList();
        for (E entity : assignedEntities) {
            entity.setModuleId(null);
            entityManager.merge(entity);
        }
    }

}
