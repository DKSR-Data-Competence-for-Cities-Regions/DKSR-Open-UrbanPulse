package de.urbanpulse.cep;

import com.espertech.esper.client.*;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.transfer.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CEPCommandHandler extends CommandHandler {

    static final String KEY_EVENT_TYPES = "eventTypes";
    static final String KEY_LISTENERS = "listeners";
    static final String KEY_VIRTUAL_SENSORS = "virtualSensors";
    static final String KEY_STATEMENTS = "statements";
    static final String KEY_RESULT_STATEMENT = "resultStatement";
    static final String KEY_RESULT_EVENT_TYPE = "resultEventType";
    static final String KEY_VIRTUAL_SENSOR_ID = "virtualSensorId";
    static final String KEY_TARGETS = "targets";
    static final String KEY_ID = "id";
    static final String STATEMENT_NAME = "statementName";

    private final static Logger LOG = LoggerFactory.getLogger(CEPCommandHandler.class);

    private final EPServiceProvider esper;
    private final CommandArgsFactory commandArgsFactory = new CommandArgsFactory();
    private final ErrorFactory errorFactory = new ErrorFactory();

    private final Map<String, EsperUpdateListener> updateListeners; //  id -> epListener
    private final Map<String, EsperUpdateListenerVertx> virtualSensorInternalUpdateListeners; // statementName -> epListenerVertx
    private final Map<String, Map<String, Object>> eventTypes; //       eventType name -> config
    private final Map<String, Map<String, Object>> virtualSensors; //   id -> vs
    private final Map<String, Integer> statementToCountOfListeners; //  statement name -> listeners count

    private  MessageProducer messageProducer;

    public CEPCommandHandler(MainVerticle mainVerticle, MessageProducer messageProducer, EPServiceProvider esper) {
        super(mainVerticle);
        this.messageProducer = messageProducer;
        this.esper = esper;
        this.eventTypes = new HashMap<>();
        this.updateListeners = new HashMap<>();
        this.virtualSensorInternalUpdateListeners = new HashMap<>();
        this.virtualSensors = new HashMap<>();
        this.statementToCountOfListeners = new HashMap<>();
    }


    public boolean setup(JsonObject configMap) {
        MultiUndoCommand dummyMultiUndoCommand = new MultiUndoCommand(LOG, this, null, null);
        CommandResult dummyCommandResult = (JsonObject, UndoCommand) -> {
        };
        clearCache();
        try{
            configMap.getJsonArray(KEY_EVENT_TYPES).stream()
                    .map(JsonObject.class::cast)
                    .map(eventType -> eventType.getMap())
                    .forEach(eventType -> registerEventType(eventType, false, dummyCommandResult));

            configMap.getJsonArray(KEY_VIRTUAL_SENSORS).stream()
                    .map(JsonObject.class::cast).map(JsonObject::getMap)
                    .forEach(virtualSensor -> registerVirtualSensorEventTypes(
                            virtualSensor, false, dummyMultiUndoCommand, dummyCommandResult));

            configMap.getJsonArray(KEY_STATEMENTS).stream()
                    .map(JsonObject.class::cast)
                    .map(statement -> statement.getMap())
                    .forEach(statement -> registerStatement(statement, false, dummyCommandResult));

            configMap.getJsonArray(KEY_VIRTUAL_SENSORS).stream()
                    .map(JsonObject.class::cast)
                    .map(JsonObject::getMap)
                    .forEach(virtualSensor -> registerVirtualSensorStatements(virtualSensor, false,
                    dummyMultiUndoCommand, dummyCommandResult));

            configMap.getJsonArray(KEY_VIRTUAL_SENSORS).stream()
                    .map(JsonObject.class::cast)
                    .map(JsonObject::getMap)
                    .forEach(virtualSensor -> virtualSensors.put((String) virtualSensor.get(KEY_VIRTUAL_SENSOR_ID), virtualSensor));

            configMap.getJsonArray(KEY_LISTENERS).stream()
                    .map(JsonObject.class::cast)
                    .map(JsonObject::getMap)
                    .forEach(listener -> registerUpdateListener(listener, false, dummyCommandResult));
            return true;
        } catch (Exception ex) {
            LOG.error("Failed to setup CEP from config.", ex);
            return false;
        }

    }


    public void registerStatement(Map<String, Object> statement, boolean createUndoCommand, CommandResult callback) {
        JsonObject config = new JsonObject(statement);
        String statementName = config.getString("name");
        JsonArray targets = config.getJsonArray(KEY_TARGETS, new JsonArray());

        LOG.debug("Starting to register statement " + statementName);
        String query = config.getString("query");

        if (esper.getEPAdministrator().getStatement(statementName) != null) {
            LOG.info("Already registered at esper. Skip processing statement " + statementName);
            callback.done(errorFactory.createErrorMessage("already registered"), null);
        } else {
            try {
                EPStatement epStatement = esper.getEPAdministrator().createEPL(query, statementName);
                LOG.info("Registered at esper statement {0}", statementName);
                if (!targets.isEmpty()) {
                    EsperUpdateListenerVertx listener = new EsperUpdateListenerVertx(super.mainVerticle.getVertx(), config, messageProducer);
                    epStatement.addListener(listener);
                    virtualSensorInternalUpdateListeners.put(statementName, listener);
                    LOG.info("Added listener {0} to statement {1}.", listener, statementName);
                }
                UndoCommand undoCommand = null;
                if (createUndoCommand) {
                    undoCommand = new UndoCommand(LOG, this, "unregisterStatement",
                            commandArgsFactory.buildArgs("name", statementName));
                }
                callback.done(new JsonObject(), undoCommand);
            } catch (EPServiceDestroyedException | EPException ex) {
                final String errorMsg =
                        String.format("Can't register query %s with name %s", query, statementName);
                LOG.error(errorMsg, ex);
                callback.done(errorFactory.createErrorMessage(errorMsg + ": " + ex.getMessage()),
                        null);
            }
        }
    }

    public void unregisterStatement(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        String statementName = (String) args.get("name");
        LOG.debug("Starting to unregister statement {0}.", statementName);
        EPStatement epStatement = esper.getEPAdministrator().getStatement(statementName);
        if (epStatement == null) {
            LOG.warn("Skip unregistering unknown statement {0}.", statementName);
            callback.done(errorFactory.createErrorMessage("unknown statement"), null);
            return;
        }

        if (virtualSensorInternalUpdateListeners.containsKey(statementName)) {
            epStatement.removeListener(virtualSensorInternalUpdateListeners.get(statementName));
            virtualSensorInternalUpdateListeners.remove(statementName);
            statementToCountOfListeners.remove(statementName);
            LOG.info("Removed statement {0} from esper and cache.", statementName);
        }

        if (epStatement.getUpdateListeners().hasNext()) {
            callback.done(errorFactory.createErrorMessage("existing listener"), null);
            return;
        }
        epStatement.destroy();
        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = commandArgsFactory.buildArgs("name", statementName, "query",
                    epStatement.getText());
            undoCommand = new UndoCommand(LOG, this, "registerStatement", undoArgs);
        }
        callback.done(new JsonObject(), undoCommand);
    }


    public void registerEventType(Map<String, Object> eventType, boolean createUndoCommand, CommandResult callback) {
        String eventTypeName = (String) eventType.get("name");
        LOG.debug("Start registering event type {0}.", eventTypeName);
        EventType et = esper.getEPAdministrator().getConfiguration().getEventType(eventTypeName);
        if (et != null) {
            LOG.info("Already registered at esper. Skip processing event type {0}.", eventTypeName);
            callback.done(errorFactory.createErrorMessage("existing eventtype"), null);
            return;
        }

        Object rawConfig = eventType.get("config");
        Map<String, Object> config;
        if (rawConfig instanceof JsonObject) {
            config = ((JsonObject) eventType.get("config")).getMap();
        } else {
            config = (Map<String, Object>) eventType.get("config");
        }
        config.put("_headers", EventParamTypes.MAP);

        try {
            registerEventTypeAtEsper(eventTypeName, config);
            eventTypes.put(eventTypeName, config);
            LOG.info("Added event type {0} to esper and cache.", eventTypeName);
        } catch (ConfigurationException e) {
            LOG.error("Unable to register event type {0}.", e, eventTypeName);
            callback.done(errorFactory.createErrorMessage(e.getLocalizedMessage()), null);
            return;
        }
        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            undoCommand = new UndoCommand(LOG, this, "unregisterEventType",
                    commandArgsFactory.buildArgs("name", eventTypeName));
        }
        callback.done(new JsonObject(), undoCommand);
    }

    public void unregisterEventType(Map<String, Object> eventType, boolean createUndoCommand, CommandResult callback) {
        String eventTypeName = (String) eventType.get("name");
        LOG.debug("Starting to unregister event type " + eventTypeName);
        boolean force = false;
        EventType et = esper.getEPAdministrator().getConfiguration().getEventType(eventTypeName);
        if (et == null) {
            LOG.warn("Skipping to unregister unknown eventtype {0}.", eventTypeName);
            callback.done(errorFactory.createErrorMessage("unknown eventtype"), null);
            return;
        }
        try {
            if (esper.getEPAdministrator().getConfiguration().removeEventType(eventTypeName, force)) {
                UndoCommand undoCommand = null;
                if (createUndoCommand) {
                    Map<String, Object> undoArgs = commandArgsFactory.buildArgs("name", eventTypeName);
                    Map<String, Object> config = new HashMap<>();
                    for (String key : et.getPropertyNames()) {
                        config.put(key, et.getPropertyType(key).getName());
                    }
                    undoArgs.put("config", config);
                    undoCommand = new UndoCommand(LOG, this, "registerEventType", undoArgs);
                }
                eventTypes.remove(eventTypeName);
                LOG.info("Removed event type {0} from esper and cache." + eventTypeName);
                callback.done(new JsonObject(), undoCommand);
            } else {
                callback.done(errorFactory.createErrorMessage("can't remove eventtype"), null);
            }
        } catch (ConfigurationException ex) {
            LOG.error("can't unregister eventtype " + eventTypeName, ex);
            callback.done(errorFactory.createErrorMessage("can't remove eventtype"), null);
        }
    }

    public void registerUpdateListener(Map<String, Object> listener, boolean createUndoCommand, CommandResult callback) {
        String listenerId = (String) listener.get(KEY_ID);
        String statementName = (String) listener.get(STATEMENT_NAME);
        LOG.debug("Starting to register update listener {0} on statement {1}.", listenerId, statementName);
        String vertxAddress = (String) listener.get("vertxAddress");
        if (updateListeners.get(listenerId) != null) {
            LOG.warn("Skipping to register existing listener {0} for statement {1}.", listener, statementName);
            callback.done(errorFactory.createErrorMessage("existing listener"), null);
            return;
        }
        EPStatement epStatement = esper.getEPAdministrator().getStatement(statementName);
        if (epStatement == null) {
            LOG.warn("Skipping to register listener {0} for in esper unknown statement {1}.", listener, statementName);
            callback.done(errorFactory.createErrorMessage("unknown statement"), null);
            return;
        }
        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            undoCommand = new UndoCommand(LOG, this, "unregisterUpdateListener",
                    commandArgsFactory.buildArgs(KEY_ID, listenerId));
        }
        if (statementToCountOfListeners.containsKey(statementName)) {
            statementToCountOfListeners.put(statementName, statementToCountOfListeners.get(statementName) + 1);
            if (statementToCountOfListeners.get(statementName) > 1) {
                callback.done(new JsonObject(), undoCommand);
                return;
            }
        } else {
            statementToCountOfListeners.put(statementName, 1);
        }
        // only, if no listener already registered.
        EsperUpdateListener espListener = new EsperUpdateListener(listenerId, statementName, vertxAddress, messageProducer);
        updateListeners.put(listenerId, espListener);
        epStatement.addListener(espListener);
        LOG.info("Added listenerId {0} for statement {1} to esper and cache.", listenerId, statementName);
        callback.done(new JsonObject(), undoCommand);
    }


    public void unregisterUpdateListener(Map<String, Object> listener, boolean createUndoCommand, CommandResult callback) {
        LOG.debug("Starting to unregister update listener.");
        String listenerId, statementName;
        if (listener.containsKey(STATEMENT_NAME)) {
            listenerId = getUpdateListenerIdByStatementName((String) listener.get(STATEMENT_NAME));
            statementName = (String) listener.get(STATEMENT_NAME);
        } else if (listener.containsKey(KEY_ID)) {
            listenerId = (String) listener.get(KEY_ID);
            statementName = updateListeners.get(listenerId).getStatement();
        } else {
            LOG.warn("Skipping to unregister unknown listener, args do neither contain statementName nor id");
            callback.done(errorFactory.createErrorMessage("args do neither contain statementName nor id"), null);
            return;
        }
        if (listenerId == null) {
            LOG.warn("Skipping to unregister unknown listener, id was null.");
            callback.done(errorFactory.createErrorMessage("unknown listener id"), null);
            return;
        }
        EsperUpdateListener espListener = updateListeners.get(listenerId);
        if (espListener == null) {
            LOG.warn("Skipping to unregister listener {0}. Unknown in esper.", listener);
            callback.done(errorFactory.createErrorMessage("unknown listener"), null);
            return;
        }
        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            Map<String, Object> undoArgs = commandArgsFactory.buildArgs(KEY_ID, espListener.getId());
            undoArgs.put(STATEMENT_NAME, espListener.getStatement());
            undoArgs.put("vertxAddress", espListener.getOutboundDestination());
            undoCommand = new UndoCommand(LOG, this, "registerUpdateListener", undoArgs);
        }
        if (statementToCountOfListeners.containsKey(statementName)) {
            statementToCountOfListeners.put(statementName, statementToCountOfListeners.get(statementName) - 1);
            if (statementToCountOfListeners.get(statementName) > 0) {
                LOG.info("Removed listener from statement {0}. Now {1} listners remaining.",
                    statementName, statementToCountOfListeners.get(statementName));
                callback.done(new JsonObject(), undoCommand);
                return;
            }
        }
        // only called, if no listener left
        EPStatement es = esper.getEPAdministrator().getStatement(espListener.getStatement());
        if (es != null) {
            es.removeListener(espListener);
        }
        updateListeners.remove(listenerId);
        LOG.info("Removed last listener from statement {0}. Removed esper listener for statement.", statementName);
        callback.done(new JsonObject(), undoCommand);
    }

    public void registerVirtualSensor(Map<String, Object> virtualSensor, boolean createUndoCommand, final CommandResult callback) {
        String virtualSensorId = (String) virtualSensor.get(KEY_VIRTUAL_SENSOR_ID);

        MultiUndoCommand multiUndoCommand = new MultiUndoCommand(LOG, this, null, null);

        if (!registerVirtualSensorEventTypes(virtualSensor, createUndoCommand, multiUndoCommand, callback)) {
            return;
        }

        if (!registerVirtualSensorStatements(virtualSensor, createUndoCommand, multiUndoCommand, callback)) {
            return;
        }

        // Remember virtual sensor for unregister
        virtualSensors.put(virtualSensorId, virtualSensor);
        LOG.info("Registered virtual sensor {0}.", virtualSensor);
        // Since everything was successful, an undo just unregisters the virtual sensor again (we don't need the multi undo anymore)
        final UndoCommand undoCommand = createUndoCommand ? new UndoCommand(LOG, this, "unregisterVirtualSensor",
                Collections.singletonMap(KEY_VIRTUAL_SENSOR_ID, virtualSensorId)) : null;
        callback.done(new JsonObject(), undoCommand);
    }


    public void updateVirtualSensorTargets(Map<String, Object> args, boolean createUndoCommand, final CommandResult callback) {
        String resultStatementName = (String) args.get("resultStatementName");
        String targetsArrayString = (String) args.get(KEY_TARGETS);
        JsonArray targets = new JsonArray(targetsArrayString);

        EsperUpdateListenerVertx vertxUpdateListener = virtualSensorInternalUpdateListeners.get(resultStatementName);
        if (vertxUpdateListener == null) {
            LOG.error("Error when trying to update VS targets: No UpdateListener for VS with resultStatement {0} found.", resultStatementName);
            callback.done(errorFactory.createErrorMessage("VirtualSensor UpdateListener not found"), null);
            return;
        }

        vertxUpdateListener.setTargets(targets);
        LOG.info("Updated VS with resultStatement {0}. New targets: {1}.", resultStatementName, targets);
        Map<String, Object> undoArgs = new HashMap<>();
        undoArgs.put(KEY_TARGETS, vertxUpdateListener.getTargets());
        undoArgs.put("resultStatementName", resultStatementName);

        final UndoCommand undoCommand = createUndoCommand ? new UndoCommand(LOG, this, "updateVirtualSensorTargets",
                undoArgs) : null;

        callback.done(new JsonObject(), undoCommand);
    }

    private boolean registerVirtualSensorStatements(Map<String, Object> args, boolean createUndoCommand,
            MultiUndoCommand multiUndoCommand, final CommandResult callback) {
        // Register statements (including result statement)
        List<Map<String, Object>> statements = (List<Map<String, Object>>) args.get(KEY_STATEMENTS);
        Map<String, Object> resultStatement = (Map<String, Object>) args.get(KEY_RESULT_STATEMENT);
        statements = new ArrayList<>(statements);
        statements.add(resultStatement);
        AtomicReference<JsonObject> resultWrapper = new AtomicReference<>();
        for (Map<String, Object> statement : statements) {
            registerStatement(statement, createUndoCommand, (JsonObject result, UndoCommand cmd) -> {
                resultWrapper.set(result);
                if (isSuccessful(result)) {
                    multiUndoCommand.add(cmd);
                }
            });
            if (!isSuccessful(resultWrapper)) {
                // Revert and fail
                callback.done(resultWrapper.get(), multiUndoCommand);
                return false;
            }
        }
        return true;
    }

    private boolean registerVirtualSensorEventTypes(Map<String, Object> virtualSensor, boolean createUndoCommand,
            MultiUndoCommand multiUndoCommand, final CommandResult callback) {
        // Register event types (including result event type)
        List<Map<String, Object>> eventTypez = (List<Map<String, Object>>) virtualSensor.get(KEY_EVENT_TYPES);
        Map<String, Object> resultEventType = (Map<String, Object>) virtualSensor.get(KEY_RESULT_EVENT_TYPE);
        eventTypez = new ArrayList<>(eventTypez);
        eventTypez.add(resultEventType);
        AtomicReference<JsonObject> resultWrapper = new AtomicReference<>();
        for (Map<String, Object> eventType : eventTypez) {
            registerEventType(eventType, createUndoCommand, (JsonObject result, UndoCommand cmd) -> {
                resultWrapper.set(result);
                if (isSuccessful(result)) {
                    multiUndoCommand.add(cmd);
                }
            });
            if (!isSuccessful(resultWrapper)) {
                // Revert and fail
                callback.done(resultWrapper.get(), multiUndoCommand);
                return false;
            }
        }
        return true;
    }

    Map<String, Object> getVirtualSensor(String virtualSensorId) {
        return virtualSensors.get(virtualSensorId);
    }


    public void unregisterVirtualSensor(Map<String, Object> virtualSensor, boolean createUndoCommand, final CommandResult callback) {
        String virtualSensorId = (String) virtualSensor.get(KEY_VIRTUAL_SENSOR_ID);
        Map<String, Object> cachedVirtualSensor = virtualSensors.get(virtualSensorId);
        if (cachedVirtualSensor == null) {
            LOG.warn("Skip unregistering unknown virtual sensor {0}.", virtualSensor);
            callback.done(errorFactory.createErrorMessage("Virtual sensor with id " + virtualSensorId + " not found."), null);
            return;
        }

        MultiUndoCommand multiUndoCommand = new MultiUndoCommand(LOG, this, null, null);
        if (!unregisterVirtualSensorStatements(cachedVirtualSensor, createUndoCommand, multiUndoCommand, callback)) {
            return;
        }

        if (!unregisterVirtualSensorEventTypes(cachedVirtualSensor, createUndoCommand, multiUndoCommand, callback)) {
            return;
        }

        // Forget virtual sensor
        virtualSensors.remove(virtualSensorId);
        LOG.info("Removed virtual sensor {0} from esper and cache.", virtualSensor);

        // Since everything was successful, an undo just registers the virtual sensor again (we don't need the multi undo anymore)
        final UndoCommand undoCommand = createUndoCommand ? new UndoCommand(LOG, this, "registerVirtualSensor", cachedVirtualSensor) : null;
        callback.done(new JsonObject(), undoCommand);
    }

    private boolean unregisterVirtualSensorEventTypes(Map<String, Object> virtualSensor, boolean createUndoCommand,
            MultiUndoCommand multiUndoCommand, final CommandResult callback) {
        // Unregister event types (including result event type)
        List<Map<String, Object>> eventTypez = (List<Map<String, Object>>) virtualSensor.get(KEY_EVENT_TYPES);
        Map<String, Object> resultEventType = (Map<String, Object>) virtualSensor.get(KEY_RESULT_EVENT_TYPE);
        eventTypez = new ArrayList<>(eventTypez);
        eventTypez.add(resultEventType);
        AtomicReference<JsonObject> resultWrapper = new AtomicReference<>();
        for (Map<String, Object> eventType : eventTypez) {
            unregisterEventType(eventType, createUndoCommand, (JsonObject result, UndoCommand cmd) -> {
                resultWrapper.set(result);
                if (isSuccessful(result)) {
                    multiUndoCommand.add(cmd);
                }
            });
            if (!isSuccessful(resultWrapper)) {
                // Revert and fail
                callback.done(resultWrapper.get(), multiUndoCommand);
                return false;
            }
        }
        return true;
    }

    private boolean unregisterVirtualSensorStatements(Map<String, Object> virtualSensor, boolean createUndoCommand,
            MultiUndoCommand multiUndoCommand, final CommandResult callback) {
        // Unregister statements (including result statement)
        List<Map<String, Object>> statements = (List<Map<String, Object>>) virtualSensor.get(KEY_STATEMENTS);
        Map<String, Object> resultStatement = (Map<String, Object>) virtualSensor.get(KEY_RESULT_STATEMENT);
        statements = new ArrayList<>(statements);
        statements.add(resultStatement);
        AtomicReference<JsonObject> resultWrapper = new AtomicReference<>();
        for (Map<String, Object> statement : statements) {
            unregisterStatement(statement, createUndoCommand, (JsonObject result, UndoCommand cmd) -> {
                resultWrapper.set(result);
                if (isSuccessful(result)) {
                    multiUndoCommand.add(cmd);
                }
            });
            if (!isSuccessful(resultWrapper)) {
                // Revert and fail
                callback.done(resultWrapper.get(), multiUndoCommand);
                return false;
            }
        }
        return true;
    }

    private boolean isSuccessful(AtomicReference<JsonObject> result) {
        return isSuccessful(result.get());
    }

    private boolean isSuccessful(JsonObject result) {
        return result.isEmpty();
    }


    public void countProcessedEvents(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOG.info("Counting processed events");
        Long count = esper.getEPRuntime().getNumEventsEvaluated();
        JsonObject result = new JsonObject();
        result.put("processedEvents", count);
        callback.done(result, null);
    }


    public void reset(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOG.info("Starting to reset the CEP.");
        updateListeners.values().forEach(erw -> {
            String statementName = erw.getStatement();
            EPStatement es = esper.getEPAdministrator().getStatement(statementName);
            if (es != null) {
                es.removeListener(erw);
            }
        });
        esper.getEPAdministrator().destroyAllStatements();
        LOG.info("Destroyed all statements in esper.");
        for (EventType et : esper.getEPAdministrator().getConfiguration().getEventTypes()) {
            esper.getEPAdministrator().getConfiguration().removeEventType(et.getName(), true);
            LOG.info("Removed eventType {0} in esper.", et.getName());
        }
        clearCache();
        callback.done(new JsonObject(), null);
    }

    Map<String, Map<String, Object>> getEventTypes() {
        return eventTypes;
    }

    boolean isEventTypeRegistered(String eventTypeName) {
        return esper.getEPAdministrator().getConfiguration().isEventTypeExists(eventTypeName);
    }


    public void getStatements(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOG.trace("Getting statements");
        JsonObject statementMap = new JsonObject();
        for (String statementName : esper.getEPAdministrator().getStatementNames()) {
            EPStatement statement = esper.getEPAdministrator().getStatement(statementName);
            statementMap.put(statement.getName(), statement.getText());
        }
        callback.done(statementMap, null);
    }

    boolean isStatementRegistered(String statementName) {
        return Arrays.asList(esper.getEPAdministrator().getStatementNames()).contains(statementName);
    }


    public void getUpdateListenerCount(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOG.trace("Getting update listener count");
        JsonObject reply = new JsonObject();
        String statementName = (String) args.get("name");
        EPStatement statement = esper.getEPAdministrator().getStatement(statementName);
        if (Objects.isNull(statement)) {
            LOG.warn("Stop getUpdateListenerCount. Tried to get statement {0}. Unknown in esper.", statementName);
            callback.done(errorFactory.createErrorMessage("unknown statement"), null);
            return;
        }
        int listenerCount = 0;
        if (statementToCountOfListeners.containsKey(statementName)) {
            listenerCount = statementToCountOfListeners.get(statementName);
        }
        reply.put("listenerCount", listenerCount);
        callback.done(reply, null);
    }


    public void getEventTypes(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOG.trace("Getting event types");
        JsonObject reply = new JsonObject();
        JsonArray eventTypeNames = new JsonArray();
        for (EventType eventType : esper.getEPAdministrator().getConfiguration().getEventTypes()) {
            eventTypeNames.add(eventType.getName());
        }
        reply.put(KEY_EVENT_TYPES, eventTypeNames);
        callback.done(reply, null);
    }

    private void registerEventTypeAtEsper(String eventTypeName, Map<String, Object> config) {
        final Properties p = new Properties();
        config.forEach((String key, Object val) -> {
            p.setProperty(key, val.toString());
            LOG.debug(key + " -> " + val);
        });
        esper.getEPAdministrator().getConfiguration().addEventType(eventTypeName, p);
    }

    void clearCache() {
        updateListeners.clear();
        eventTypes.clear();
        statementToCountOfListeners.clear();
        virtualSensors.clear();
        virtualSensorInternalUpdateListeners.clear();
        LOG.info("Cleared cached VS, update listeners, event types.");
    }

    /**
     * @return the statementToCountOfListeners
     */
    public Map<String, Integer> getStatementToCountOfListeners() {
        return statementToCountOfListeners;
    }

    /**
     * @return the updateListeners
     */
    public Map<String, EsperUpdateListener> getUpdateListeners() {
        return updateListeners;
    }

    private String getUpdateListenerIdByStatementName(String statementName) {
        for (Entry<String, EsperUpdateListener> entry : updateListeners.entrySet()) {
            if (entry.getValue().getStatement().equals(statementName)) {
                return entry.getValue().getId();
            }
        }
        return null;
    }

    protected Map<String, EsperUpdateListenerVertx> getVirtualSensorInternalUpdateListeners() {
        return virtualSensorInternalUpdateListeners;
    }

    public void init() {
        esper.initialize();
    }

    public void sendEvent(Map<String, Object> eventAsMap, String eventTypeName) {
        esper.getEPRuntime().sendEvent(eventAsMap, eventTypeName);
    }

}
