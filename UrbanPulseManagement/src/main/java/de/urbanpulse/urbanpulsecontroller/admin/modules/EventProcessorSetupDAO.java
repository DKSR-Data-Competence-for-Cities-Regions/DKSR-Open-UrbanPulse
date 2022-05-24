package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.UpdateListenerEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UpdateListenerDAO;
import de.urbanpulse.urbanpulsecontroller.admin.VirtualSensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
public class EventProcessorSetupDAO implements ModuleSetup {

    public static final String OUTBOUND_VERTX_ADDRESS = "theOutbound";

    @EJB
    private EventTypeManagementDAO eventTypeManagementDAO;

    @EJB
    private StatementManagementDAO statementManagementDAO;

    @EJB
    private VirtualSensorManagementDAO virtualSensorsDAO;

    @EJB
    private UpdateListenerDAO listenerDAO;

    /**
     *
     * @param module
     * @param setup
     * @return the setup
     * @deprecated
     */
    @Deprecated
    public JsonObject createEventProcessorSetup(UPModuleEntity module, JsonObject setup) {
        return createModuleSetup(module, setup);
    }

    @Override
    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {

        List<VirtualSensorEntity> virtualSensors = virtualSensorsDAO.queryAll();

        JsonArray eventTypes = createCepEventTypes(virtualSensors);
        setup.put("eventTypes", eventTypes);

        JsonArray statements = createCepStatements(virtualSensors);
        setup.put("statements", statements);

        JsonArray virtualSensorz = createVirtualSensors(virtualSensors);
        setup.put("virtualSensors", virtualSensorz);

        JsonArray listeners = createCepListeners();
        setup.put("listeners", listeners);

        return setup;
    }

    private JsonArray createCepEventTypes(List<VirtualSensorEntity> virtualSensors) {
        List<EventTypeEntity> eventTypeEntities = eventTypeManagementDAO.queryAll();

        Set<String> virtualSensorEventTypes = virtualSensors
                .stream()
                .map(virtualSensor ->
                        new JsonArray(virtualSensor.getEventTypeIds())
                            .add(virtualSensor.getResultEventType().getId()))
                .flatMap(JsonArray::stream)
                .map(String.class::cast)
                .collect(Collectors.toSet());

        return new JsonArray(eventTypeEntities
                .stream()
                .filter(to -> !virtualSensorEventTypes.contains(to.getId()))
                .map(this::eventTypeToJson)
                .collect(Collectors.toList()));
    }

    private JsonObject eventTypeToJson(EventTypeEntity et) {
        String name = et.getName();
        JsonObject config = new JsonObject(et.getEventParameter());
        JsonObject eventType = new JsonObject();
        eventType.put("name", name);
        eventType.put("config", config);
        return eventType;
    }

    JsonArray createCepStatements(List<VirtualSensorEntity> virtualSensors) {
        List<StatementEntity> statementEntities = statementManagementDAO.queryAll();

        Set<String> virtualSensorStatements = virtualSensors
                .stream()
                .map(virtualSensor ->
                    new JsonArray(virtualSensor.getStatementIds())
                            .add(virtualSensor.getResultStatement().getId()))
                .flatMap(JsonArray::stream)
                .map(String.class::cast)
                .collect(Collectors.toSet());

        return new JsonArray(statementEntities
                .stream()
                .filter(to -> !virtualSensorStatements.contains(to.getId()))
                .map(this::statementToJson)
                .collect(Collectors.toList()));
    }

    private JsonObject statementToJson(StatementEntity to) {
        String name = to.getName();
        String query = to.getQuery();
        JsonObject statement = new JsonObject();
        statement.put("name", name);
        statement.put("query", query);
        statement.put("id", to.getId());
        return statement;
    }

    private JsonObject statementToJson(StatementEntity to, String targetsString) {
        JsonObject statement = statementToJson(to);
        JsonArray targets = (targetsString != null) ? new JsonArray(targetsString) : new JsonArray();
        statement.put("targets", targets);
        return statement;
    }

    private JsonArray createCepListeners() {
        List<UpdateListenerEntity> listenerEntities = listenerDAO.queryAll();
        return new JsonArray(listenerEntities.stream().map(to -> {
            String id = "" + to.getId();
            String statementId = to.getStatement().getId();
            StatementEntity statement = statementManagementDAO.queryById(statementId);
            String statementName = statement.getName();
            JsonObject listener = new JsonObject();
            listener.put("id", id);
            listener.put("statementName", statementName);
            listener.put("vertxAddress", OUTBOUND_VERTX_ADDRESS);
            return listener;
        }).collect(Collectors.toList()));
    }

    private JsonArray createVirtualSensors(List<VirtualSensorEntity> virtualSensors) {
        return new JsonArray(virtualSensors
                .stream()
                .map(this::virtualSensorToJson)
                .collect(Collectors.toList()));
    }

    private JsonObject virtualSensorToJson(VirtualSensorEntity virtualSensor) {
        JsonArray eventTypes = new JsonArray(
                new JsonArray(virtualSensor.getEventTypeIds())
                        .stream()
                        .map(String.class::cast)
                        .map(eventTypeManagementDAO::queryById)
                        .filter(Objects::nonNull)
                        .map(this::eventTypeToJson)
                        .collect(Collectors.toList()));

        JsonObject resultEventType = eventTypeToJson(virtualSensor.getResultEventType());

        JsonArray statements = new JsonArray(
                new JsonArray(virtualSensor.getStatementIds())
                        .stream()
                        .map(String.class::cast)
                        .map(statementManagementDAO::queryById)
                        .filter(Objects::nonNull)
                        .map(this::statementToJson)
                        .collect(Collectors.toList()));

        JsonObject resultStatement = statementToJson(virtualSensor.getResultStatement(), virtualSensor.getTargets());

        String virtualSensorId = virtualSensor.getId();

        return new JsonObject()
                .put("eventTypes", eventTypes)
                .put("resultEventType", resultEventType)
                .put("statements", statements)
                .put("resultStatement", resultStatement)
                .put("virtualSensorId", virtualSensorId);
    }

    @Override
    public UPModuleType getModuleType() {
        return UPModuleType.EventProcessor;
    }

}
