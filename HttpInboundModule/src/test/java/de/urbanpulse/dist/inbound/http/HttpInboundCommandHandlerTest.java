package de.urbanpulse.dist.inbound.http;

import static de.urbanpulse.dist.inbound.http.HttpInboundCommandHandler.CONNECTOR_AUTH_MAP_NAME;
import static de.urbanpulse.dist.inbound.http.HttpInboundCommandHandler.SENSOR_EVENT_TYPES_MAP_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import de.urbanpulse.transfer.UndoCommand;
import io.vertx.core.Vertx;
import io.vertx.core.json.*;
import io.vertx.core.logging.*;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.junit.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class HttpInboundCommandHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpInboundCommandHandlerTest.class);
    private static final String sid1 = "Sid1";
    private static final String eventType1 = "EventType1";
    private static final String sid2 = "Sid2";
    private static final String eventType2 = "EventType2";
    private static final String connectorId1 = "1";
    private static final String connectorKey1 = "key1";

    private Vertx vertx;
    private HttpInboundCommandHandler commandHandler;

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();


    @Before
    public void setUp() {
        vertx = rule.vertx();
        vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME).clear();
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).clear();

        commandHandler = new HttpInboundCommandHandler(new MainVerticle() {
            @Override
            public Vertx getVertx() {
                return rule.vertx();
            }
        });
    }

    @Test
    public void bug_2863_invocationTargetExceptionTest() {
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).clear();
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).put(sid1, eventType1);

        LOGGER.info("start");
        LocalMap<String, String> map0 = vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
        for (String key : map0.keySet()) {
            LOGGER.info(key + "->" + map0.get(key));
            assertEquals(key, sid1);
            assertEquals(map0.get(key), eventType1);
        }

        Map<String, Object> serverArgs = new HashMap<>();

        final String eventType = "EventType2";

        boolean useJson = false;
        if (!useJson) {
            Map<String, String> eventTypeMap = new HashMap<>();
            eventTypeMap.put(sid2, eventType);
            serverArgs.put("sensorEventTypes", eventTypeMap);
        } else {
            JsonObject eventTypeMap2 = new JsonObject();
            eventTypeMap2.put(sid2, eventType);
            Object d = eventTypeMap2.getMap();
            serverArgs.put("sensorEventTypes", d);
        }

        commandHandler.setSensorEventTypes(serverArgs, true, (JsonObject result, UndoCommand undoCmd) -> {
            LOGGER.info("set");
            LocalMap<String, String> map1 = vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
            for (String key : map1.keySet()) {
                LOGGER.info(key + "->" + map1.get(key));
                assertEquals(key, sid2);
                assertEquals(map1.get(key), eventType2);
            }
            commandHandler.setSensorEventTypes(undoCmd.getArgs(), false, (JsonObject result1, UndoCommand cmd) -> {
                LOGGER.info("unset");
                LocalMap<String, String> map2 = vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
                for (String key : map2.keySet()) {
                    LOGGER.info(key + "->" + map2.get(key));
                    assertEquals(key, sid1);
                    assertEquals(map2.get(key), eventType1);
                }
                assertEquals(null, cmd);
            });
        });
    }

    @Test
    public void bug_2884_regressionTest() {
        String connectorId2 = "2";
        String connectorKey2 = "key2";

        vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME).put(connectorId1, connectorKey1);

        Map<String, Object> serverArgs = new HashMap<>();

        JsonObject connectorAuth = new JsonObject();
        connectorAuth.put(connectorId1, connectorKey1);
        connectorAuth.put(connectorId2, connectorKey2);

        serverArgs.put("connectorAuth", connectorAuth.getMap());

        commandHandler.registerConnector(serverArgs, true, (JsonObject result, UndoCommand undoCmd) -> {
            LOGGER.info("registerConnector");
            LocalMap<String, String> authMap = vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME);
            assertEquals(2, authMap.size());
            assertEquals(connectorKey1, authMap.get(connectorId1));
            assertEquals(connectorKey2, authMap.get(connectorId2));
        });
    }

    @Test
    public void registerSensor_success() {
        JsonObject arguments = new JsonObject().put("eventTypeName", eventType1).put("sensorId", sid1);

        commandHandler.registerSensor(arguments.getMap(), true, (JsonObject result, UndoCommand undoCmd) -> {
            LOGGER.info("register sensor");
            LocalMap<String, String> map1 = vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
            for (String key : map1.keySet()) {
                LOGGER.info(key + "->" + map1.get(key));
                assertEquals(sid1, key);
                assertEquals(eventType1, map1.get(key));
            }
            assertEquals(sid1, undoCmd.getArgs().get("sensorId"));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerSensor_error() {
        JsonObject arguments = new JsonObject().put("sensorId", sid1);
        commandHandler.registerSensor(arguments.getMap(), true, (JsonObject result, UndoCommand undoCmd) -> {
        });
    }

    @Test
    public void unregisterSensor_create_command() {
        JsonObject arguments = new JsonObject().put("sensorId", sid1);
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).clear();
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).put(sid1, eventType1);

        commandHandler.unregisterSensor(arguments.getMap(), true, (JsonObject result, UndoCommand undoCmd) -> {
            assertEquals(sid1, undoCmd.getArgs().get("sensorId"));
            assertEquals(sid1, undoCmd.getArgs().get("sensorId"));
        });
    }

    @Test
    public void updateSensor() {
        String eventType2 = "TestEventType2";
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).clear();
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).put(sid1, eventType1);

        JsonObject command = new JsonObject().put("sensorId", sid1).put("eventTypeName", eventType2);

        commandHandler.updateSensor(command.getMap(), false, (JsonObject result, UndoCommand undoCmd) -> {
            LocalMap<String, String> map1 = vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
            assertEquals(1, map1.size());
            assertEquals(eventType2, map1.get(sid1));
        });
    }

    @Test
    public void updateConnector() {
        String connectorKey2 = "key2";

        vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME).put(connectorId1, connectorKey1);

        JsonObject connectorAuth = new JsonObject();
        connectorAuth.put("connectorAuth", new JsonObject().put(connectorId1, connectorKey2));

        commandHandler.updateConnector(connectorAuth.getMap(), false, (JsonObject result, UndoCommand undoCmd) -> {
            LocalMap<String, String> authMap = vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME);
            assertEquals(1, authMap.size());
            assertEquals(connectorKey2, authMap.get(connectorId1));
        });
    }

    @Test
    public void unregisterConnector() {
        vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME).put(connectorId1, connectorKey1);

        JsonObject connectorAuth = new JsonObject();
        connectorAuth.put("connectorId", connectorId1);

        commandHandler.unregisterConnector(connectorAuth.getMap(), false, (JsonObject result, UndoCommand undoCmd) -> {
            LocalMap<String, String> authMap = vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME);
            assertEquals(0, authMap.size());
            assertNull(authMap.get(connectorId1));
        });
    }

}
