package de.urbanpulse.cep;

import com.espertech.esper.client.EPServiceProviderManager;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.TransferStructureFactory;
import de.urbanpulse.transfer.UndoCommand;
import de.urbanpulse.utils.ConfigGenerator;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CEPCommandHandlerTest {

    private static final String[] EVENT_TYPE_NAMES = new String[]{"DummyEventType1", "DummyEventType2", "DummyEventType3"};
    private static final int UPDATE_LISTENER_COUNT = 3;
    private static final String KEY_NAME = "name";

    private static CommandResult dummyCommandResult;
    private static CommandResult verifySuccessCommandResult;

    private CEPCommandHandler commandHandler;

    @Mock
    private MainVerticle mainVerticle;


    @Mock
    private MessageProducer messageProducer;

    private Map<String, Object> setupConfig = new HashMap<>();

    String virtualSensorId = "virtually-anything";
    Map<String, Object> virtualSensorRegisterArgs;
    Map<String, Object> virtualSensorResultEventType;
    Map<String, Object> virtualSensorResultStatement;
    List<Map<String, Object>> virtualSensorEventTypes;
    List<Map<String, Object>> virtualSensorStatements;

    Map<String, Object> virtualSensorUnregisterArgs = new HashMap<>();

    @BeforeClass
    public static void setUpClass() {
        dummyCommandResult = (JsonObject, UndoCommand) -> {
        };
        verifySuccessCommandResult = (JsonObject res, UndoCommand undo) -> {
            assertTrue(res.encode(), res.isEmpty());
        };
    }

    @Before
    public void setUp() {
        for (String eventType : CEPCommandHandlerTest.EVENT_TYPE_NAMES) {
            setupConfig = ConfigGenerator.joinConfigs(setupConfig,
                    ConfigGenerator.getSetupConfig(CEPCommandHandlerTest.UPDATE_LISTENER_COUNT, eventType));
        }

        setupConfig.put("virtualSensors", new JsonArray());

        ConfigGenerator.index = 1;
        when(mainVerticle.getVertx()).thenReturn(Vertx.vertx());
        commandHandler = new CEPCommandHandler(mainVerticle, messageProducer, EPServiceProviderManager.getDefaultProvider());
        commandHandler.setup(new JsonObject(Json.encode(setupConfig)));

        Map<String, Object> someData = ConfigGenerator.getSetupConfig(0, "ResultEventType");
        virtualSensorResultEventType = ((List<Map<String, Object>>) someData.get("eventTypes")).get(0);
        virtualSensorResultStatement = ((List<Map<String, Object>>) someData.get("statements")).get(0);

        Map<String, Object> someMoreData = ConfigGenerator.getSetupConfig(0, "AnotherEventType");
        virtualSensorEventTypes = (List<Map<String, Object>>) someMoreData.get("eventTypes");
        virtualSensorStatements = (List<Map<String, Object>>) someMoreData.get("statements");

        virtualSensorRegisterArgs = new HashMap<>();
        virtualSensorRegisterArgs.put("eventTypes", virtualSensorEventTypes);
        virtualSensorRegisterArgs.put("resultEventType", virtualSensorResultEventType);
        virtualSensorRegisterArgs.put("statements", virtualSensorStatements);
        virtualSensorRegisterArgs.put("resultStatement", virtualSensorResultStatement);
        virtualSensorRegisterArgs.put("virtualSensorId", virtualSensorId);

        virtualSensorUnregisterArgs.put("virtualSensorId", virtualSensorId);
    }

    @After
    public void tearDown() {
        commandHandler.reset(Collections.emptyMap(), false, dummyCommandResult);
    }

    @Test
    public void test_setup_returns_false_if_exception() {
        boolean result = commandHandler.setup(null);
        assertFalse(result);
    }

    @Test
    public void testRegisterUpdateListeners() {
        for (String eventType : CEPCommandHandlerTest.EVENT_TYPE_NAMES) {
            assertEquals(CEPCommandHandlerTest.UPDATE_LISTENER_COUNT,
                    (int) commandHandler.getStatementToCountOfListeners().get(eventType + "Statement"));
        }
        assertEquals(CEPCommandHandlerTest.EVENT_TYPE_NAMES.length, commandHandler.getUpdateListeners().size());

        String additionalTestStatement = CEPCommandHandlerTest.EVENT_TYPE_NAMES[0] + "Statement";
        commandHandler.registerUpdateListener(ConfigGenerator.getUpdateListenerConfig(additionalTestStatement, "",
                (UPDATE_LISTENER_COUNT * EVENT_TYPE_NAMES.length) + 1), false, dummyCommandResult);
        assertEquals(CEPCommandHandlerTest.EVENT_TYPE_NAMES.length, commandHandler.getUpdateListeners().size());
        assertEquals(CEPCommandHandlerTest.UPDATE_LISTENER_COUNT + 1,
                (int) commandHandler.getStatementToCountOfListeners().get(additionalTestStatement));
    }

    @Test
    public void testUnRegisterUpdateListeners() {
        for (int i = 0; i < CEPCommandHandlerTest.EVENT_TYPE_NAMES.length; i++) {
            String statementName = CEPCommandHandlerTest.EVENT_TYPE_NAMES[i] + "Statement";
            // check here if the number of registered update listeners has decreased as expected
            assertEquals(CEPCommandHandlerTest.EVENT_TYPE_NAMES.length - i, commandHandler.getUpdateListeners().size());
            for (int j = 0; j < CEPCommandHandlerTest.UPDATE_LISTENER_COUNT; j++) {
                commandHandler.unregisterUpdateListener(ConfigGenerator.getUpdateListenerConfig(statementName, "", null), false,
                        dummyCommandResult);
            }
            // at this point all statements of this kind should be unregistered
            assertEquals(0, (int) commandHandler.getStatementToCountOfListeners().get(statementName));
        }
        // at this point all update listeners should be unregistered
        assertEquals(0, commandHandler.getUpdateListeners().size());
    }

    @Test
    public void testReset() {
        String additionalTestStatement = CEPCommandHandlerTest.EVENT_TYPE_NAMES[0] + "Statement";
        commandHandler.registerUpdateListener(ConfigGenerator.getUpdateListenerConfig(additionalTestStatement, "",
                (UPDATE_LISTENER_COUNT * EVENT_TYPE_NAMES.length) + 1), false, dummyCommandResult);
        commandHandler.reset(Collections.emptyMap(), false, dummyCommandResult);

        assertTrue(commandHandler.getUpdateListeners().isEmpty());
        assertTrue(commandHandler.getStatementToCountOfListeners().isEmpty());
        commandHandler.getStatements(Collections.emptyMap(), false, (JsonObject result, UndoCommand cmd) -> {
            assertTrue(result.isEmpty());
        });
        commandHandler.getEventTypes(Collections.emptyMap(), false, (JsonObject result, UndoCommand cmd) -> {
            assertTrue(result.getJsonArray("eventTypes").isEmpty());
        });
    }

    @Test
    public void testRegisterStatementWithTarget() {
        Map<String, Object> statement = createValidStatement();
        commandHandler.registerStatement(statement, true, dummyCommandResult);
        assertTrue(commandHandler.getVirtualSensorInternalUpdateListeners().containsKey("Test_with_taget"));

        // register 2nd time should not work
        commandHandler.registerStatement(statement, true, (response, undoCommand) -> {
            String errorMsg = response.getJsonObject(TransferStructureFactory.TAG_BODY).getString(ErrorFactory.ERROR_MESSAGE_TAG);
            assertEquals("already registered", errorMsg);
        });

    }

    @Test
    public void test_unregisterStatement_cleans_maps_if_needed() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        JsonObject config = new JsonObject(createValidStatement());
        commandHandler.registerStatement(config.getMap(), true, dummyCommandResult);
        String statementName = config.getString(KEY_NAME);

        Field virtualSensorInternalUpdateListeners = commandHandler.getClass().getDeclaredField("virtualSensorInternalUpdateListeners");
        virtualSensorInternalUpdateListeners.setAccessible(true);
        ((HashMap<String, EsperUpdateListenerVertx>) virtualSensorInternalUpdateListeners.get(commandHandler))
                .put(statementName, new EsperUpdateListenerVertx(null, config));

        commandHandler.unregisterStatement(config.getMap(), true, (response, undoCommand) -> {
            String errorMsg = response.getJsonObject(TransferStructureFactory.TAG_BODY).getString(ErrorFactory.ERROR_MESSAGE_TAG);
            Map<String, EsperUpdateListenerVertx> vsiul = null;
            try {
                vsiul = (HashMap<String, EsperUpdateListenerVertx>) virtualSensorInternalUpdateListeners.get(commandHandler);
            } catch (IllegalArgumentException | IllegalAccessException ex ) {
                assertFalse("Should not get here.", true);
            }
            assertNotNull(vsiul);
            assertFalse(vsiul.containsKey(statementName));
        });
    }

    @Test
    public void test_unregisterStatement_unknown_statement() throws NoSuchFieldException {
        Map<String, Object> statement = new HashMap<>();
        statement.put(KEY_NAME, "UnknownStatement");
        commandHandler.unregisterStatement(statement, true, (response, undoCommand) -> {
            String errorMsg = response.getJsonObject(TransferStructureFactory.TAG_BODY).getString(ErrorFactory.ERROR_MESSAGE_TAG);
            assertEquals("unknown statement", errorMsg);
        });
    }

    @Test
    public void test_registerEventType_twice() {
        Map<String, Object> eventType = new HashMap<>();
        eventType.put(KEY_NAME, "TestEventType");
        eventType.put("config", new JsonObject());
        commandHandler.registerEventType(eventType, true, (response, undoCommand) -> {
            assertEquals(new JsonObject(), response);
            commandHandler.registerEventType(eventType, true, (response2, undoCommand2) -> {
                String errorMsg = response2.getJsonObject(TransferStructureFactory.TAG_BODY).getString(ErrorFactory.ERROR_MESSAGE_TAG);
                assertEquals("existing eventtype", errorMsg);
            });
        });
    }

    @Test
    public void test_unregisterEventType_unknown_eventType() {
        Map<String, Object> eventType = new HashMap<>();
        eventType.put(KEY_NAME, "UnknownEventType");
        commandHandler.unregisterEventType(eventType, true, (response, undoCommand) -> {
            String errorMsg = response.getJsonObject(TransferStructureFactory.TAG_BODY).getString(ErrorFactory.ERROR_MESSAGE_TAG);
            assertEquals("unknown eventtype", errorMsg);
        });
    }

    @Test
    public void test_registerUnregisterVirtualSensor_succeeds_ifInputOkay() {
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, false, verifySuccessCommandResult);

        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNotNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    @Test
    public void test_registerVirtualSensor_canBeRolledBack_ifEventTypeRegistrationFails() {
        // Mess up an event type...
        ((Map<String, Object>) virtualSensorResultEventType.get("config")).put("property", "This class does not exist");

        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, true, (JsonObject res, UndoCommand undoCommand) -> {
            assertFalse(res.isEmpty()); // There's a failure

            undoCommand.execute(verifySuccessCommandResult);

            // Now everything should have been unregistered again
            assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
            assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
            assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
            assertNull(commandHandler.getVirtualSensor(virtualSensorId));
        });
    }

    @Test
    public void test_registerVirtualSensor_canBeRolledBack_ifStatementRegistrationFails() {
        // Mess up a statement
        virtualSensorResultStatement.put("query", "This query is bonkers");

        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, true, (JsonObject res, UndoCommand undoCommand) -> {
            assertFalse(res.isEmpty()); // There's a failure

            undoCommand.execute(verifySuccessCommandResult);

            // Now everything should have been unregistered again
            assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
            assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
            assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
            assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
            assertNull(commandHandler.getVirtualSensor(virtualSensorId));
        });
    }

    @Test
    public void test_registerVirtualSensor_canBeRolledBack_ifSucceeds() {
        AtomicReference<UndoCommand> undoCommand = new AtomicReference<>();
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, true, (JsonObject result, UndoCommand cmd) -> {
            undoCommand.set(cmd);
        });

        undoCommand.get().execute(verifySuccessCommandResult);

        assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    @Test
    public void test_unregisterVirtualSensor_succeeds_ifInputOkay() {
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, false, verifySuccessCommandResult);

        commandHandler.unregisterVirtualSensor(virtualSensorUnregisterArgs, false, verifySuccessCommandResult);

        assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertFalse(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertFalse(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertFalse(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    @Test
    public void test_unregisterVirtualSensor_canBeRolledBack_ifStatementUnregistrationFails() {
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, false, verifySuccessCommandResult);

        //Statement unregistration can e.g. fail if there's an "external" listener attached (that doesn't belong to the VS).
        Map<String, Object> externalUpdateListenerRegisterArgs = new HashMap<>();
        externalUpdateListenerRegisterArgs.put("id", "the-update-listener-id");
        externalUpdateListenerRegisterArgs.put("statementName", virtualSensorResultStatement.get(KEY_NAME));
        externalUpdateListenerRegisterArgs.put("vertxAddress", "theTestOutbound");
        commandHandler.registerUpdateListener(externalUpdateListenerRegisterArgs, false, dummyCommandResult);

        // Now we try to unregister the VS. This should fail
        AtomicReference<UndoCommand> undoCommand = new AtomicReference<>();
        commandHandler.unregisterVirtualSensor(virtualSensorRegisterArgs, true, (JsonObject result, UndoCommand cmd) -> {
            undoCommand.set(cmd);
            assertFalse(result.isEmpty());
        });

        // Now we execute the rollback.
        undoCommand.get().execute(verifySuccessCommandResult);

        // After the rollback, everything should be as if we just registered the VS
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNotNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    @Test
    public void test_unregisterVirtualSensor_canBeRolledBack_ifEventTypeUnregistrationFails() {
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, false, verifySuccessCommandResult);

        //Event type unregistration can e.g. fail if there is a statement still registered referencing it (that doesn't belong to the VS).
        Map<String, Object> externalStatementRegisterArgs = new HashMap<>();
        externalStatementRegisterArgs.put(KEY_NAME, "MessupStatement");
        externalStatementRegisterArgs.put("targets", Collections.emptyList());
        externalStatementRegisterArgs.put("query", "select * from " + virtualSensorResultEventType.get(KEY_NAME));
        commandHandler.registerStatement(externalStatementRegisterArgs, false, dummyCommandResult);

        // Now we try to unregister the VS. This should fail
        AtomicReference<UndoCommand> undoCommand = new AtomicReference<>();
        commandHandler.unregisterVirtualSensor(virtualSensorRegisterArgs, true, (JsonObject result, UndoCommand cmd) -> {
            undoCommand.set(cmd);
            assertFalse(result.isEmpty());
        });

        // Now we execute the rollback.
        undoCommand.get().execute(verifySuccessCommandResult);

        // After the rollback, everything should be as if we just registered the VS
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNotNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    @Test
    public void test_unregisterVirtualSensor_canBeRolledBack_ifSucceeds() {
        //register, unregister, execute undo of unregister - should be like just registered
        commandHandler.registerVirtualSensor(virtualSensorRegisterArgs, false, verifySuccessCommandResult);

        AtomicReference<UndoCommand> undoCommand = new AtomicReference<>();
        commandHandler.unregisterVirtualSensor(virtualSensorUnregisterArgs, true, (JsonObject result, UndoCommand cmd) -> {
            undoCommand.set(cmd);
            assertTrue(result.isEmpty());
        });

        // Now we undo the unregister
        undoCommand.get().execute(verifySuccessCommandResult);

        // After the rollback, everything should be as if we just registered the VS
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.isEventTypeRegistered((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertTrue(commandHandler.isStatementRegistered((String) virtualSensorStatements.get(0).get(KEY_NAME)));
        assertNotNull(commandHandler.getVirtualSensor(virtualSensorId));
    }

    private Map<String, Object> createValidStatement() {
        Map<String, Object> statement = new HashMap<>();
        statement.put("query", "select * from DummyEventType1");
        statement.put(KEY_NAME, "Test_with_taget");
        List<String> targets = new ArrayList<>();
        targets.add("thePersistence");
        statement.put("targets", targets);
        return statement;
    }

}
