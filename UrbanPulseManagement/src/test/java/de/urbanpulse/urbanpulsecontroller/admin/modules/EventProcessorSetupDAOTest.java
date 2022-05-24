package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UpdateListenerDAO;
import de.urbanpulse.urbanpulsecontroller.admin.VirtualSensorManagementDAO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class EventProcessorSetupDAOTest {

    @Mock
    private EventTypeManagementDAO eventTypeManagementDAOMock;

    @Mock
    private StatementManagementDAO statementManagementDAOMock;

    @Mock
    private VirtualSensorManagementDAO virtualSensorsDAOMock;

    @Mock
    private UpdateListenerDAO listenerDAOMock;

    @InjectMocks
    private EventProcessorSetupDAO eventProcessorSetupDAO;

    private EventTypeEntity regularEventType;
    private EventTypeEntity virtualSensorIntermediateEventType;
    private EventTypeEntity virtualSensorResultEventType;
    private StatementEntity regularStatement;
    private StatementEntity virtualSensorIntermediateStatement;
    private StatementEntity virtualSensorResultStatement;
    private List<StatementEntity> statementEntities;
    private List<EventTypeEntity> eventTypeEntities;
    private VirtualSensorEntity virtualSensorEntity;
    private List<VirtualSensorEntity> virtualSensors;

    @Before
    public void setUp() {
        regularEventType = new EventTypeEntity();
        regularEventType.setId("ID");
        regularEventType.setName("Regular event type");
        regularEventType.setEventParameter(new JsonObject().encode());
        virtualSensorIntermediateEventType = new EventTypeEntity();
        virtualSensorIntermediateEventType.setId("the-intermediate-event-type-id");
        virtualSensorIntermediateEventType.setName("the-intermediate-event-type-name");
        virtualSensorIntermediateEventType.setEventParameter(new JsonObject().encode());
        virtualSensorResultEventType = new EventTypeEntity();
        virtualSensorResultEventType.setId("the-result-event-type-id");
        virtualSensorResultEventType.setName("the-result-event-type-name");
        virtualSensorResultEventType.setEventParameter(new JsonObject().encode());

        regularStatement = new StatementEntity();
        regularStatement.setId("ID");
        regularStatement.setName("Regular statement");
        regularStatement.setQuery("Regular query");
        virtualSensorIntermediateStatement = new StatementEntity();
        virtualSensorIntermediateStatement.setId("the-intermediate-statement-id");
        virtualSensorIntermediateStatement.setName("the-intermediate-statement-name");
        virtualSensorIntermediateStatement.setQuery("the-intermediate-statement-query");
        virtualSensorResultStatement = new StatementEntity();
        virtualSensorResultStatement.setId("the-result-statement-id");
        virtualSensorResultStatement.setName("the-result-statement-name");
        virtualSensorResultStatement.setQuery("the-result-statement-query");
        statementEntities = new ArrayList<>();
        statementEntities.add(regularStatement);
        statementEntities.add(virtualSensorIntermediateStatement);
        statementEntities.add(virtualSensorResultStatement);

        eventTypeEntities = new ArrayList<>();
        eventTypeEntities.add(regularEventType);
        eventTypeEntities.add(virtualSensorIntermediateEventType);
        eventTypeEntities.add(virtualSensorResultEventType);

        virtualSensorEntity = new VirtualSensorEntity();
        virtualSensorEntity.setEventTypeIds(new JsonArray().add(virtualSensorIntermediateEventType.getId()).encode());
        virtualSensorEntity.setResultEventType(virtualSensorResultEventType);
        virtualSensorEntity.setStatementIds(new JsonArray().add(virtualSensorIntermediateStatement.getId()).encode());
        virtualSensorEntity.setResultStatement(virtualSensorResultStatement);
        virtualSensorEntity.setTargets(new JsonArray().add("thePersistence").encode());

        virtualSensors = new ArrayList<>();
        virtualSensors.add(virtualSensorEntity);
    }

    /**
     * Test of createModuleSetup method, of class EventProcessorSetupDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateModuleSetup() throws Exception {

        given(statementManagementDAOMock.queryAll()).willReturn(statementEntities);
        given(statementManagementDAOMock.queryById(virtualSensorIntermediateStatement.getId())).willReturn(virtualSensorIntermediateStatement);

        given(eventTypeManagementDAOMock.queryAll()).willReturn(eventTypeEntities);

        given(eventTypeManagementDAOMock.queryById(virtualSensorIntermediateEventType.getId())).willReturn(virtualSensorIntermediateEventType);

        given(virtualSensorsDAOMock.queryAll()).willReturn(virtualSensors);

        JsonObject config = eventProcessorSetupDAO.createModuleSetup(null, new JsonObject());

        // Assert the virtual sensor is included at all
        assertTrue(config.containsKey("virtualSensors"));
        assertEquals(1, config.getJsonArray("virtualSensors").size());

        // Assert that (only) regular statements are in the top-level of the config
        assertTrue(config.containsKey("statements"));
        assertEquals(1, config.getJsonArray("statements").size());
        assertEquals(regularStatement.getId(), getByPath(config, "statements/0/id"));
        assertEquals(regularStatement.getName(), getByPath(config, "statements/0/name"));
        assertEquals(regularStatement.getQuery(), getByPath(config, "statements/0/query"));

        // Assert the intermediate statement is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("statements"));
        assertEquals(1, config.getJsonArray("virtualSensors").getJsonObject(0).getJsonArray("statements").size());
        assertEquals(virtualSensorIntermediateStatement.getId(), getByPath(config, "virtualSensors/0/statements/0/id"));
        assertEquals(virtualSensorIntermediateStatement.getName(), getByPath(config, "virtualSensors/0/statements/0/name"));
        assertEquals(virtualSensorIntermediateStatement.getQuery(), getByPath(config, "virtualSensors/0/statements/0/query"));

        // Assert the virtual sensor result statement is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("resultStatement"));
        assertEquals(virtualSensorResultStatement.getId(), getByPath(config, "virtualSensors/0/resultStatement/id"));
        assertEquals(virtualSensorResultStatement.getName(), getByPath(config, "virtualSensors/0/resultStatement/name"));
        assertEquals(virtualSensorResultStatement.getQuery(), getByPath(config, "virtualSensors/0/resultStatement/query"));

        // Assert that listeners are attached to result statement of the virtual sensor
        assertEquals("thePersistence", getByPath(config, "virtualSensors/0/resultStatement/targets/0"));

        // Assert that (only) regular event types are in the top-level of the config
        assertTrue(config.containsKey("eventTypes"));
        assertEquals(1, config.getJsonArray("eventTypes").size());
        assertEquals(regularEventType.getName(), getByPath(config, "eventTypes/0/name"));
        assertEquals(regularEventType.getEventParameter(), getByPath(config, "eventTypes/0/config").toString());

        // Assert the intermediate event type is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("eventTypes"));
        assertEquals(1, config.getJsonArray("virtualSensors").getJsonObject(0).getJsonArray("eventTypes").size());
        assertEquals(virtualSensorIntermediateEventType.getName(), getByPath(config, "virtualSensors/0/eventTypes/0/name"));
        assertEquals(virtualSensorIntermediateEventType.getEventParameter(),
                getByPath(config, "virtualSensors/0/eventTypes/0/config").toString());

        // Assert the VS result event type is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("resultEventType"));
        assertEquals(virtualSensorResultEventType.getName(), getByPath(config, "virtualSensors/0/resultEventType/name"));
        assertEquals(virtualSensorResultEventType.getEventParameter(),
                getByPath(config, "virtualSensors/0/resultEventType/config").toString());
    }

    @Test
    public void test_createModuleSetup_willFilterDeletedStatementsAndEventTypes() {
        given(statementManagementDAOMock.queryAll()).willReturn(Arrays.asList(regularStatement, virtualSensorResultStatement));

        given(statementManagementDAOMock.queryById(virtualSensorIntermediateStatement.getId())).willReturn(null);

        given(eventTypeManagementDAOMock.queryAll()).willReturn(Arrays.asList(regularEventType, virtualSensorResultEventType));

        given(eventTypeManagementDAOMock.queryById(virtualSensorIntermediateEventType.getId())).willReturn(null);

        given(virtualSensorsDAOMock.queryAll()).willReturn(virtualSensors);

        JsonObject config = eventProcessorSetupDAO.createModuleSetup(null, new JsonObject());

        // Assert the virtual sensor is included at all
        assertTrue(config.containsKey("virtualSensors"));
        assertEquals(1, config.getJsonArray("virtualSensors").size());

        // Assert that (only) regular statements are in the top-level of the config
        assertTrue(config.containsKey("statements"));
        assertEquals(1, config.getJsonArray("statements").size());
        assertEquals(regularStatement.getId(), getByPath(config, "statements/0/id"));
        assertEquals(regularStatement.getName(), getByPath(config, "statements/0/name"));
        assertEquals(regularStatement.getQuery(), getByPath(config, "statements/0/query"));

        // Assert the intermediate statement is not contained in the virtual sensor config anymore
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("statements"));
        assertEquals(0, config.getJsonArray("virtualSensors").getJsonObject(0).getJsonArray("statements").size());

        // Assert the virtual sensor result statement is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("resultStatement"));
        assertEquals(virtualSensorResultStatement.getId(), getByPath(config, "virtualSensors/0/resultStatement/id"));
        assertEquals(virtualSensorResultStatement.getName(), getByPath(config, "virtualSensors/0/resultStatement/name"));
        assertEquals(virtualSensorResultStatement.getQuery(), getByPath(config, "virtualSensors/0/resultStatement/query"));

        // Assert that listeners are attached to result statement of the virtual sensor
        assertEquals("thePersistence", getByPath(config, "virtualSensors/0/resultStatement/targets/0"));

        // Assert that regular event types are in the top-level of the config
        assertTrue(config.containsKey("eventTypes"));
        assertEquals(1, config.getJsonArray("eventTypes").size());
        assertEquals(regularEventType.getName(), getByPath(config, "eventTypes/0/name"));
        assertEquals(regularEventType.getEventParameter(), getByPath(config, "eventTypes/0/config").toString());

        // Assert the intermediate event type is not contained in the virtual sensor config anymore
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("eventTypes"));
        assertEquals(0, config.getJsonArray("virtualSensors").getJsonObject(0).getJsonArray("eventTypes").size());

        // Assert the VS result event type is contained in the virtual sensor config
        assertTrue(config.getJsonArray("virtualSensors").getJsonObject(0).containsKey("resultEventType"));
        assertEquals(virtualSensorResultEventType.getName(), getByPath(config, "virtualSensors/0/resultEventType/name"));
        assertEquals(virtualSensorResultEventType.getEventParameter(),
                getByPath(config, "virtualSensors/0/resultEventType/config").toString());
    }

    // Helper function -> traverses the json structure to get a certain value
    private Object getByPath(JsonObject jsonObject, String pathExpression) {
        String[] path = pathExpression.split("/");
        Object o = jsonObject;
        for (String key : path) {
            if (o instanceof JsonObject) {
                o = ((JsonObject) o).getValue(key);
            } else if (o instanceof JsonArray) {
                o = ((JsonArray) o).getValue(Integer.valueOf(key));
            } else {
                throw new IllegalArgumentException("Can't find " + pathExpression);
            }
        }
        return o;
    }
}
