package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import io.vertx.core.json.JsonArray;

import java.util.LinkedList;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import static javax.ejb.TransactionAttributeType.MANDATORY;

import javax.inject.Inject;
import javax.persistence.Query;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class VirtualSensorManagementDAO extends AbstractUUIDDAO<VirtualSensorEntity, VirtualSensorTO> {

    @Inject
    private CategoryManagementDAO categoryManagmentDAO;

    @Inject
    private StatementManagementDAO statementManagementDAO;


    public VirtualSensorManagementDAO() {
        super(VirtualSensorEntity.class, VirtualSensorTO.class);
    }

    public List<VirtualSensorTO> getFilteredByResultStatementName(String resultStatementName) {
        Query query = entityManager.createNamedQuery("getByResultStatementName");
        query.setParameter("resultStatementName", resultStatementName);
        List<VirtualSensorEntity> entities = query.getResultList();
        return toTransferObjectList(entities);
    }

    public List<VirtualSensorTO> getFilteredByCategory(String categoryId) {
        final CategoryEntity category = categoryManagmentDAO.queryById(categoryId);
        if (category == null) {
            return new LinkedList<>();
        }
        return getFilteredBy("category", category);
    }


    public VirtualSensorTO createVirtualSensorAndReplaceSidPlaceholder(String categoryId, String resultStatementId, String statementIdsJsonString,
                                                                       String descriptionJsonString, String eventTypeIdJsonString, String resultEventTypeId, List<String> targets) throws ReferencedEntityMissingException {

        VirtualSensorEntity virtualSensor = new VirtualSensorEntity();

        virtualSensor.setTargets(new JsonArray(targets).toString());

        virtualSensor.setDescription(descriptionJsonString);

        virtualSensor.setStatementIds(statementIdsJsonString);

        virtualSensor.setEventTypeIds(eventTypeIdJsonString);

        CategoryEntity category = queryCategory(categoryId);
        virtualSensor.setCategory(category);

        StatementEntity resultStatement = queryStatement(resultStatementId);
        EventTypeEntity resultEventType = queryEventTypeEntityById(resultEventTypeId);

        entityManager.persist(virtualSensor); // required to set the ID

        replacePlaceholderInQueryWithSid(virtualSensor.getId(), resultStatement);
        virtualSensor.setResultEventType(resultEventType);
        virtualSensor.setResultStatement(resultStatement);

        virtualSensor = entityManager.merge(virtualSensor);
        entityManager.flush();

        return new VirtualSensorTO(virtualSensor);
    }

    public void updateVirtualSensorTargets(String sid, String targets) {
        VirtualSensorEntity entity = entityManager.find(VirtualSensorEntity.class, sid);
        entity.setTargets(targets);
    }

    private EventTypeEntity queryEventTypeEntityById(String id) throws ReferencedEntityMissingException {
        EventTypeEntity result = entityManager.find(EventTypeEntity.class, id);
        if (null == result) {
            throw new ReferencedEntityMissingException("virtualsensor references missing eventtype with id[" + id + "]");
        }
        return result;
    }

    private StatementEntity queryStatement(String id) throws ReferencedEntityMissingException {
        StatementEntity entity = statementManagementDAO.queryById(id);
        if (entity == null) {
            throw new ReferencedEntityMissingException("virtualsensor references missing statement with id[" + id + "]");
        }

        return entity;
    }

    private CategoryEntity queryCategory(String id) throws ReferencedEntityMissingException {
        CategoryEntity entity = categoryManagmentDAO.queryById(id);
        if (entity == null) {
            throw new ReferencedEntityMissingException("virtualsensor references missing category with id[" + id + "]");
        }

        return entity;
    }


    private void replacePlaceholderInQueryWithSid(String virtualSensorId, StatementEntity resultStatement) {
        String queryTemplate = resultStatement.getQuery();
        String query = queryTemplate.replace("<SID_PLACEHOLDER>", virtualSensorId);
        resultStatement.setQuery(query);
        entityManager.merge(resultStatement);
    }
}
