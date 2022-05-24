package de.urbanpulse.dist.inbound.http;

import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.UndoCommand;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class HttpInboundCommandHandler extends CommandHandler {

    private static final  Logger LOGGER = LoggerFactory.getLogger(HttpInboundCommandHandler.class);
    private static final  String CONNECTOR_AUTH_FIELD = "connectorAuth";
    private static final  String SENSOR_ID_FIELD = "sensorId";
    private final ErrorFactory errorFactory = new ErrorFactory();

    /**
     * shared map: maps SID to JsonArray of eventType names (as String
     * representation)
     */
    public static String SENSOR_EVENT_TYPES_MAP_NAME = "SensorEventTypesMap";

    /**
     * shared map: maps connector ID to its HMAC key
     */
    public static String CONNECTOR_AUTH_MAP_NAME = "ConnectorAuthMap";

    private final LocalMap<String, String> sensorEventTypesMap;
    private final LocalMap<String, String> connectorAuthMap;

    HttpInboundCommandHandler(MainVerticle mainVerticle) {
        super(mainVerticle);
        this.sensorEventTypesMap = mainVerticle.getVertx().sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME);
        this.connectorAuthMap = mainVerticle.getVertx().sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME);
    }

    public void setup(JsonObject configMap) {
        createSensorEventTypesMap(configMap.getJsonObject("sensorEventTypes"));
        createConnectorAuthMap(configMap.getJsonObject(CONNECTOR_AUTH_FIELD));
    }

    private void createConnectorAuthMap(JsonObject connectorAuthSetup) {
        LOGGER.info("creating connectorAuthMap");
        connectorAuthSetup.stream().forEach(entry -> {
            String connectorId = entry.getKey();
            LOGGER.info("processing entry " + connectorId);
            if (entry.getValue() instanceof String) {
                String hmacKey = (String) entry.getValue();
                this.connectorAuthMap.put(connectorId, hmacKey);
            }
        });
    }

    private void removeConnectorAuth(String connectorId) {
        LOGGER.info("removing from connectorAuthMap");
        this.connectorAuthMap.remove(connectorId);
    }

    private void createSensorEventTypesMap(JsonObject paramSensorEventTypes) {
        sensorEventTypesMap.clear();
        if (paramSensorEventTypes != null) {
            paramSensorEventTypes.forEach(e -> {
                if (e.getValue() instanceof String) {
                    final String sid = e.getKey();
                    final String eventType = (String) e.getValue();
                    sensorEventTypesMap.put(sid, eventType);
                }
            });
        }
    }

    public void registerSensor(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("registerSensor called");

        JsonObject argsAsJson = new JsonObject(args);
        String sensorId = argsAsJson.getString(SENSOR_ID_FIELD);
        Optional<String> eventTypeName = Optional.ofNullable(argsAsJson.getString("eventTypeName"));

        if (sensorEventTypesMap.keySet().contains(sensorId)) {
            callback.done(errorFactory.createErrorMessage("HttpInboundCommandHandler: sensor is already registered: " + sensorId),
                    null);
            return;
        }

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put(SENSOR_ID_FIELD, sensorId);
            undoCommand = new UndoCommand(LOGGER, this, "unregisterSensor", undoArgs);
        }

        if(eventTypeName.isPresent()) {
            sensorEventTypesMap.put(sensorId, eventTypeName.get());
        } else {
            throw new IllegalArgumentException("the parameter 'eventTypeName' could not be found");
        }
        callback.done(new JsonObject(), undoCommand);
    }

    public void updateSensor(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("updateSensor called");
        JsonObject argsAsJson = new JsonObject(args);
        String sensorId = argsAsJson.getString(SENSOR_ID_FIELD);
        Optional<String> eventTypeName = Optional.ofNullable(argsAsJson.getString("eventTypeName"));

        if (!sensorEventTypesMap.keySet().contains(sensorId)) {
            callback.done(errorFactory.createErrorMessage("HttpInboundCommandHandler: cannot find sensor with id: " + sensorId),
                    null);
            return;
        }

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put(SENSOR_ID_FIELD, sensorId);
            undoArgs.put("eventTypeName", sensorEventTypesMap.get(sensorId));
            undoCommand = new UndoCommand(LOGGER, this, "updateSensor", undoArgs);
        }

        if(eventTypeName.isPresent()) {
            sensorEventTypesMap.put(sensorId, eventTypeName.get());
        } else {
            throw new IllegalArgumentException("the parameter 'eventTypeName' could not be found");
        }
        callback.done(new JsonObject(), undoCommand);
    }

    public void unregisterSensor(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("unregisterSensor called");
        String sensorId = (String) args.get(SENSOR_ID_FIELD);

        if (!sensorEventTypesMap.keySet().contains(sensorId)) {
            callback.done(errorFactory.createErrorMessage("HttpInboundCommandHandler: cannot find sensor with id: " + sensorId),
                    null);
            return;
        }

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put(SENSOR_ID_FIELD, sensorId);
            undoArgs.put("eventTypeName", sensorEventTypesMap.get(sensorId));
            undoCommand = new UndoCommand(LOGGER, this, "registerSensor", undoArgs);
        }

        sensorEventTypesMap.remove(sensorId);
        callback.done(new JsonObject(), undoCommand);
    }

    public void registerConnector(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("registerConnector called");

        // the args value may be a JsonObject or a Map<String, String>
        JsonObject argsAsJson = new JsonObject(args);
        JsonObject connectorAuthJsonObj = argsAsJson.getJsonObject(CONNECTOR_AUTH_FIELD);
        Optional<String> connectorId = connectorAuthJsonObj.fieldNames().stream().findAny();

        UndoCommand undoCommand = null;
        if (createUndoCommand && connectorId.isPresent()) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put("connectorId", connectorId);
            undoCommand = new UndoCommand(LOGGER, this, "unregisterConnector", undoArgs);
        }

        createConnectorAuthMap(connectorAuthJsonObj);
        callback.done(new JsonObject(), undoCommand);
    }

    public void updateConnector(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("updateConnector called");

        // the args value may be a JsonObject or a Map<String, String>
        JsonObject argsAsJson = new JsonObject(args);
        JsonObject connectorAuthJsonObj = argsAsJson.getJsonObject(CONNECTOR_AUTH_FIELD);
        Optional<String> connectorId = connectorAuthJsonObj.fieldNames().stream().findAny();

        UndoCommand undoCommand = null;
        if (createUndoCommand && connectorId.isPresent()) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put(CONNECTOR_AUTH_FIELD, new JsonObject().put(connectorId.get(), connectorAuthMap.get(connectorId.get())));
            undoCommand = new UndoCommand(LOGGER, this, "updateConnector", undoArgs);
        }

        createConnectorAuthMap(connectorAuthJsonObj);
        callback.done(new JsonObject(), undoCommand);
    }

    public void unregisterConnector(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("unregisterConnector called");

        String connectorId = (String) args.get("connectorId");

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = new HashMap<>();
            undoArgs.put(CONNECTOR_AUTH_FIELD, new JsonObject().put(connectorId, connectorAuthMap.get(connectorId)));
            undoCommand = new UndoCommand(LOGGER, this, "registerConnector", undoArgs);
        }

        connectorAuthMap.remove(connectorId);
        callback.done(new JsonObject(), undoCommand);
    }

    public void setSensorEventTypes(Map<String, Object> sensorEventtypesMapping, boolean createUndoCommand, CommandResult callback) {
        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = new HashMap<>();
            JsonObject sETypes = new JsonObject();
            for (String sid : sensorEventTypesMap.keySet()) {
                sETypes.put(sid, sensorEventTypesMap.get(sid));
            }
            undoArgs.put("sensorEventType", sETypes);
            undoCommand = new UndoCommand(LOGGER, this, "setSensorEventType", undoArgs);
        }
        JsonObject sensorEventtypesMappingAsJson = new JsonObject(sensorEventtypesMapping);
        // the args value may be a JsonObject or a Map<String, List<String>>
        JsonObject sensorEventTypesJsonObj = sensorEventtypesMappingAsJson.getJsonObject("sensorEventTypes");

        createSensorEventTypesMap(sensorEventTypesJsonObj);

        callback.done(new JsonObject(), undoCommand);
    }
}
