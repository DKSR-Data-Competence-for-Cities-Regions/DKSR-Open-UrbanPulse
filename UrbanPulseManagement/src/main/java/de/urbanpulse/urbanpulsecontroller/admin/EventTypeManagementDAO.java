package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.EventTypeException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import javax.persistence.PersistenceException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
@TransactionAttribute(MANDATORY)
public class EventTypeManagementDAO extends AbstractUUIDDAO<EventTypeEntity, EventTypeTO> {

    public EventTypeManagementDAO() {
        super(EventTypeEntity.class, EventTypeTO.class);
    }

    public boolean eventTypeExists(String name) {
        return !queryFilteredBy("name", name).isEmpty();
    }

    /**
     * create a new {@link EventTypeTO} and persist it in the database
     *
     * @param name unique name of the event type
     * @param description description JSON
     * @param config event configuration
     * @return id string or null if error
     */
    public EventTypeTO createEventType(String name, String description, String config) {
        try {
            EventTypeEntity entity = new EventTypeEntity();
            entity.setDescription(description);
            entity.setName(name);
            entity.setEventParameter(config);
            entityManager.persist(entity);
            entityManager.flush();
            return toTransferObject(entity);
        } catch (PersistenceException e) {
            String msg = "failed to create entity with name[" + name + "]";
            Logger.getLogger(EventTypeManagementDAO.class.getName()).log(Level.SEVERE,
                    msg, e);
            return null;
        }
    }

    /**
     * updates an existing event type
     *
     * @param id unique id of the event type to update
     * @param name unique name of the event type
     * @param description description JSON
     * @param config event configuration
     * @return status of update
     */
    public EventTypeTO updateEventType(String id, String name, String description, String config) {
        try {
            EventTypeEntity entity = queryById(id);
            if (entity == null) {
                return null;
            }

            boolean isModified = false;

            if (name != null && !name.isEmpty()) {
                entity.setName(name);
                isModified = true;
            }

            if (description != null && !description.isEmpty()) {
                entity.setDescription(description);
                isModified = true;
            }

            if (config != null && !config.isEmpty()) {
                entity.setEventParameter(config);
                isModified = true;
            }

            if (isModified) {
                entityManager.persist(entity);
                entityManager.flush();
            }

            return toTransferObject(entity);
        } catch (PersistenceException e) {
            String msg = "failed to create entity with name[" + name + "]";
            Logger.getLogger(EventTypeManagementDAO.class.getName()).log(Level.SEVERE,
                    msg, e);
            return null;
        }
    }

    @Override
    public String deleteById(String id) {
        EventTypeEntity entity = queryById(id);
        if (null == entity) {
            return null;
        }

        if (doSensorsExistForType(entity)) {
            throw new EventTypeException("Eventtype can not be deleted because sensors exists");
        }

        entityManager.remove(entity);
        entityManager.flush();
        return id;
    }

    private boolean doSensorsExistForType(EventTypeEntity entity) {
        return (entity.getSensors()!=null) && !entity.getSensors().isEmpty();
    }

    public List<EventTypeEntity> queryByIdList(List<String> eventTypeIds) throws ReferencedEntityMissingException {
        List<EventTypeEntity> eventTypes =
                eventTypeIds.stream().map(this::queryById).filter(Objects::nonNull).collect(Collectors.toList());
        if (eventTypes.size() != eventTypeIds.size()) {
            throw new ReferencedEntityMissingException("did not find at least one referenced event type");
        }
        return eventTypes;
    }
}
