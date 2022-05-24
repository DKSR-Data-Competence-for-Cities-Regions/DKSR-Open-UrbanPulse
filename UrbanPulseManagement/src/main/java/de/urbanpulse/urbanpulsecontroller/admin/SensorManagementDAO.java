package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import javax.inject.Inject;
import javax.naming.OperationNotSupportedException;
import javax.persistence.Query;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class SensorManagementDAO extends AbstractUUIDDAO<SensorEntity, SensorTO> {

    @Inject
    private ConnectorManagementDAO connectorDao;

    @Inject
    private EventTypeManagementDAO eventTypeDao;

    @Inject
    private CategoryManagementDAO categoryDao;

    public SensorManagementDAO() {
        super(SensorEntity.class, SensorTO.class);
    }

    /**
     * @return all sensor entities, but with their event types and categoris already pre-loaded in the same query
     * to avoid HORRIBLE inefficiencies of lazy loading when iterating
     * <p>
     * see http://zeroturnaround.com/rebellabs/how-to-use-jpa-correctly-to-avoid-complaints-of-a-slow-application/
     * (the best JPA how-to EVER!!!)
     */
    public List<SensorEntity> queryAllWithDepsFetched() {
        return queryAllWithDepsFetched(null);
    }

    /**
     * @param filterBySensors List of sensors which should be included in the result, all others are not returned
     * @return all sensor entities, but with their event types and categoris already pre-loaded in the same query
     * to avoid HORRIBLE inefficiencies of lazy loading when iterating
     * <p>
     * see http://zeroturnaround.com/rebellabs/how-to-use-jpa-correctly-to-avoid-complaints-of-a-slow-application/
     * (the best JPA how-to EVER!!!)
     */
    public List<SensorEntity> queryAllWithDepsFetched(List<String> filterBySensors) {
        String baseQueryStr = "select s from SensorEntity s left join fetch s.eventType left join fetch s.categories";
        Query baseQuery;

        if (filterBySensors != null && !filterBySensors.isEmpty()) {
            baseQuery = entityManager.createQuery(baseQueryStr + " where s.id in :sidList");
            baseQuery.setParameter("sidList", filterBySensors);
        } else {
            baseQuery = entityManager.createQuery(baseQueryStr);
        }

        return baseQuery.getResultList();
    }

    public List<SensorTO> getAllWithDepsFetched() {
        return toTransferObjectList(queryAllWithDepsFetched());
    }

    public List<SensorTO> getAllWithDepsFetched(List<String> filterBySensors) {
        return toTransferObjectList(queryAllWithDepsFetched(filterBySensors));
    }

    public List<SensorTO> getAllFromCategoryWithDeps(CategoryEntity category) {
        return getAllFromCategoryWithDeps(category, null);
    }

    public List<SensorTO> getAllFromCategoryWithDeps(CategoryEntity category, List<String> filterBySensors) {
        String baseQueryStr = "select s from SensorEntity s left join fetch s.categories left join fetch "
                    + "s.eventType where :category member of s.categories";
        Query baseQuery;
        if (filterBySensors != null && !filterBySensors.isEmpty()) {
            baseQuery = entityManager.createQuery(baseQueryStr + " and s.id in :sidList");
            baseQuery.setParameter("sidList", filterBySensors);
        } else {
            baseQuery = entityManager.createQuery(baseQueryStr);
        }

        baseQuery.setParameter("category", category);
        List<SensorEntity> entities = baseQuery.getResultList();
        return toTransferObjectList(entities);
    }

    /**
     * @param description the sensor's description
     * @param  categoryIds a list of with the category's ids
     * @param connectorId the id fo the connector
     * @param eventType the EventType of the sensor
     * @param locationJson string containing the location in json format
     * @return sensor
     * @throws ReferencedEntityMissingException references category or event type does not exist
     */
    public SensorTO createSensor(EventTypeEntity eventType, String connectorId,
            List<String> categoryIds, String description, String locationJson) throws ReferencedEntityMissingException {

        SensorEntity sensor = new SensorEntity();

        sensor.setDescription(description);
        sensor.setLocation(locationJson); sensor.setLocation(locationJson);

        ConnectorEntity connector = queryConnector(connectorId);
        connector.addSensor(sensor);
        sensor.setConnector(connector);
        entityManager.persist(connector);

        sensor.setEventType(eventType);

        List<CategoryEntity> categories = queryCategories(categoryIds);
        sensor.setCategories(categories);

        eventType.addSensor(sensor);
        entityManager.persist(eventType);

        for (CategoryEntity category : categories) {
            category.addSensor(sensor);
            entityManager.persist(category);
        }

        entityManager.persist(sensor);
        entityManager.flush();

        return toTransferObject(sensor);
    }

    /**
     * @param description the sensor's description
     * @param  categoryIds a list of with the category's ids
     * @param connectorId the id fo the connector
     * @param id the id of the sensor to be updated
     * @param eventTypeId the id of the event
     * @param location string representation for the location
     * @return sensor the updated sensor
     * @throws javax.naming.OperationNotSupportedException sensor to update not found
     * @throws ReferencedEntityMissingException references category or event type does not exist
     */
    public SensorTO updateSensor(String id, String eventTypeId, String connectorId, List<String> categoryIds,
            String description, String location) throws OperationNotSupportedException, ReferencedEntityMissingException {
        try {
            SensorEntity existingSensor = queryById(id);
            if (existingSensor == null) {
                throw new OperationNotSupportedException("sensor to update does not exist");
            }

            existingSensor.setDescription(description);
            existingSensor.setLocation(location);

            ConnectorEntity connector = queryConnector(connectorId);
            existingSensor.setConnector(connector);

            EventTypeEntity eventType = queryEventType(eventTypeId);
            existingSensor.setEventType(eventType);

            replaceCategories(existingSensor, categoryIds);

            SensorEntity mergedSensor = entityManager.merge(existingSensor);
            entityManager.flush();
            if (mergedSensor != null) {
                return toTransferObject(mergedSensor);
            }

        } catch (RuntimeException ex) {
            Logger.getLogger(SensorManagementDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * @return sensor
     * @param id if of the sensor to be updated
     * @param eventTypeId the id of the eventType
     * @throws javax.naming.OperationNotSupportedException sensor to update not found
     * @throws ReferencedEntityMissingException references category or event type does not exist
     */
    public SensorTO updateSensorEventTypeIds(String id, String eventTypeId)
            throws OperationNotSupportedException, ReferencedEntityMissingException {
        try {
            SensorEntity existingSensor = queryById(id);
            if (existingSensor == null) {
                throw new OperationNotSupportedException("sensor to update does not exist");
            }

            EventTypeEntity eventType = queryEventType(eventTypeId);
            existingSensor.setEventType(eventType);

            SensorEntity mergedSensor = entityManager.merge(existingSensor);
            entityManager.flush();
            if (mergedSensor != null) {
                return toTransferObject(mergedSensor);
            }

        } catch (RuntimeException ex) {
            Logger.getLogger(SensorManagementDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private void replaceCategories(SensorEntity existingSensor, List<String> categoryIds) throws ReferencedEntityMissingException {
        List<CategoryEntity> oldCategories = existingSensor.getCategories();

        for (CategoryEntity category : oldCategories) {
            category.removeSensor(existingSensor);
            entityManager.merge(category);
        }

        List<CategoryEntity> newCategories = queryCategories(categoryIds);

        existingSensor.setCategories(newCategories);

        for (CategoryEntity category : newCategories) {
            category.addSensor(existingSensor);
            entityManager.merge(category);
        }
    }

    private ConnectorEntity queryConnector(String id) throws ReferencedEntityMissingException {
        ConnectorEntity entity = connectorDao.queryById(id);
        if (entity == null) {
            throw new ReferencedEntityMissingException("sensor references missing connector (via senderid) with id[" + id + "]");
        }

        return entity;
    }

    private List<CategoryEntity> queryCategories(List<String> categoryIds) throws ReferencedEntityMissingException {
        List<CategoryEntity> entities = new LinkedList<>();
        for (String id : categoryIds) {
            CategoryEntity entity = categoryDao.queryById(id);
            if (entity == null) {
                throw new ReferencedEntityMissingException("sensor references missing category with id[" + id + "]");
            }

            entities.add(entity);
        }
        return entities;
    }

    private EventTypeEntity queryEventType(String eventTypeId) throws ReferencedEntityMissingException {
        EventTypeEntity entity = eventTypeDao.queryById(eventTypeId);
        if (entity == null) {
            throw new ReferencedEntityMissingException("sensor references missing event type with id[" + eventTypeId + "]");
        }
        return entity;
    }

    public void merge(SensorEntity entity) {
        entityManager.merge(entity);
    }

    @Override
    public String deleteById(String id) {
        SensorEntity entity = queryById(id);
        if (null == entity) {
            return null;
        }

        removeFromCategories(entity);
        removeFromConnector(entity);
        removeFromEventType(entity);

        entityManager.remove(entity);
        entityManager.flush();
        return id;
    }

    private void removeFromCategories(SensorEntity sensor) {
        List<CategoryEntity> categories = sensor.getCategories();
        for (CategoryEntity category : categories) {
            category.removeSensor(sensor);
            entityManager.merge(category);
        }
    }

    private void removeFromConnector(SensorEntity sensor) {
        ConnectorEntity connector = sensor.getConnector();
        connector.removeSensor(sensor);
        entityManager.merge(connector);
    }

    private void removeFromEventType(SensorEntity sensor) {
        EventTypeEntity eventType = sensor.getEventType();
        eventType.removeSensor(sensor);
        entityManager.merge(eventType);
    }
}
