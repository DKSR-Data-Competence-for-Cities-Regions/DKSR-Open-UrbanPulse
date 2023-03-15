package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.SensorEventTypesMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * creates JsonObjects for various commands we will send to connected vert.x modules
 *
 * NOTE: methods called "createXYZCommand" (singular!) will create only a single command object, whereas "createXYZCommands"
 * (plural!) will create a wrapper object containing the respective command object(s)
 *
 * As the structure of the returned {@link JsonObject}s differs between the two, they cannot be used interchangeably!
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class CommandsJsonFactory {

    @Inject
    private SensorEventTypesMapper sensorEventTypesMapper;

    @Inject
    private OutboundSetupDAO outboundSetup;

    public JsonObject createRegisterEventTypeCommands(String name, JsonObject config) {
        JsonObject args = new JsonObject();
        args.put("name", name);
        args.put("config", config);
        JsonObject command = createCommand("registerEventType", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }

    public JsonObject createUnregisterEventTypeCommands(String eventTypeName) {
        JsonObject refreshCommand = createSetSensorEventTypesCommand();
        JsonObject unregisterCommand = createUnregisterEventTypeCommand(eventTypeName);

        JsonArray moduleTypeCommands = new JsonArray();
        moduleTypeCommands.add(refreshCommand);
        moduleTypeCommands.add(unregisterCommand);

        JsonObject commands = new JsonObject();

        commands.put("moduleTypeCommands", moduleTypeCommands);
        return commands;
    }

    private JsonObject createUnregisterEventTypeCommand(String eventTypeName) {
        JsonObject args = new JsonObject();
        args.put("name", eventTypeName);
        JsonObject command = createCommand("unregisterEventType", args, UPModuleType.EventProcessor);
        return command;
    }
    
    public JsonObject createRegisterConnectorCommands(String connectorId, String authKey, String backchannelKey, String backchannelEndpoint) {
        JsonObject inboundCommand = createInboundConnectorCommand(connectorId, authKey, "registerConnector");
        JsonObject backchannelCommand = createBackchannelConnectorCommand(connectorId, backchannelKey, backchannelEndpoint, "registerConnector");
        return createCommandsForModuleType(inboundCommand, backchannelCommand);
    }

    public JsonObject createUpdateConnectorCommands(String connectorId, String authKey, String backchannelKey, String backchannelEndpoint) {
        JsonObject inboundCommand = createInboundConnectorCommand(connectorId, authKey, "updateConnector");
        JsonObject backchannelCommand = createBackchannelConnectorCommand(connectorId, backchannelKey, backchannelEndpoint, "updateConnector");
        return createCommandsForModuleType(inboundCommand, backchannelCommand);
    }

    private JsonObject createInboundConnectorCommand(String connectorId, String authKey, String command) {
        JsonObject connectorAuth = new JsonObject();
        connectorAuth.put(connectorId, authKey);

        JsonObject args = new JsonObject();
        args.put("connectorAuth", connectorAuth);
        return createCommand(command, args, UPModuleType.InboundInterface);
    }
    
    public JsonObject createUnregisterConnectorCommands(String connectorId) {
        JsonObject inboundCommand = createUnregisterConnectorCommand(connectorId, UPModuleType.InboundInterface);
        JsonObject backchannelCommand = createUnregisterConnectorCommand(connectorId, UPModuleType.Backchannel);
        return createCommandsForModuleType(inboundCommand, backchannelCommand);
    }
    
    public JsonObject createUnregisterConnectorCommand(String connectorId, UPModuleType type) {
        JsonObject connectorParams = new JsonObject();
        connectorParams.put("connectorId", connectorId);

        return createCommand("unregisterConnector", connectorParams, type);
    }
    
    private JsonObject createBackchannelConnectorCommand(String connectorId, String backchannelKey, String backchannelEndpoint, String command) {
        JsonObject connectorParams = new JsonObject();
        connectorParams.put("connectorId", connectorId);
        connectorParams.put("backchannelKey", backchannelKey);
        connectorParams.put("backchannelEndpoint", backchannelEndpoint);

        return createCommand(command, connectorParams, UPModuleType.Backchannel);
    }

    public JsonObject createRegisterSensorCommands(String sensorId, String eventTypeName, String connectorId) {
        JsonObject inboundArgs = new JsonObject();
        inboundArgs.put("sensorId", sensorId);
        inboundArgs.put("eventTypeName", eventTypeName);
        JsonObject inboundCommand = createCommand("registerSensor", inboundArgs, UPModuleType.InboundInterface);
        
        JsonObject backchannelArgs = new JsonObject();
        backchannelArgs.put("sensorId", sensorId);
        backchannelArgs.put("connectorId", connectorId);
        JsonObject backchannelCommand = createCommand("registerSensor", backchannelArgs, UPModuleType.Backchannel);
        
        return createCommandsForModuleType(inboundCommand, backchannelCommand);
    }

    public JsonObject createUpdateSensorCommands(String sensorId, String eventTypeName) {
        JsonObject args = new JsonObject();
        args.put("sensorId", sensorId);
        args.put("eventTypeName", eventTypeName);
        JsonObject command = createCommand("updateSensor", args, UPModuleType.InboundInterface);
        
        // The backchannel module doesn't have to be notified about sensor updates, the connector ID cannot be changed after creation
        return createCommandsForModuleType(command);
    }

    public JsonObject createUnregisterSensorCommands(String sensorId) {
        JsonObject args = new JsonObject();
        args.put("sensorId", sensorId);
        JsonObject inboundCommand = createCommand("unregisterSensor", args.copy(), UPModuleType.InboundInterface);
        JsonObject backchannelCommand = createCommand("unregisterSensor", args.copy(), UPModuleType.Backchannel);
        return createCommandsForModuleType(inboundCommand, backchannelCommand);
    }

    public JsonObject createSetSensorEventTypesCommands() {
        JsonObject command = createSetSensorEventTypesCommand();
        return createCommandsForModuleType(command);
    }

    public JsonObject createSetSensorEventTypesCommand() {
        final JsonObject sensorEventTypes = sensorEventTypesMapper.readSensorEventTypes();
        JsonObject args = new JsonObject();
        args.put("sensorEventTypes", sensorEventTypes);
        return createCommand("setSensorEventTypes", args, UPModuleType.InboundInterface);
    }

    private JsonObject createCommand(String method, JsonObject args, UPModuleType moduleType) {
        JsonObject command = new JsonObject();
        command.put("moduleType", moduleType.name());
        command.put("method", method);
        command.put("args", args);
        return command;
    }

    private JsonObject createCommandsForModuleType(JsonObject... command) {
        JsonObject commands = new JsonObject();
        JsonArray moduleTypeCommands = new JsonArray();
        Arrays.stream(command).forEach(moduleTypeCommands::add);
        commands.put("moduleTypeCommands", moduleTypeCommands);
        return commands;
    }

    public JsonObject createRegisterStatementCommands(String name, String query) {
        JsonObject args = new JsonObject();
        args.put("name", name);
        args.put("query", query);
        JsonObject command = createCommand("registerStatement", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }

    public JsonObject createUnregisterStatementCommands(String statementName) {
        JsonObject args = new JsonObject();
        args.put("name", statementName);
        JsonObject command = createCommand("unregisterStatement", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }
    
    public JsonObject createRegisterVirtualSensorCommands(String virtualSensorId, JsonArray eventTypes, JsonArray statements, JsonObject resultStatement, JsonObject resultEventType) {
        JsonObject args = new JsonObject()
                .put("eventTypes", eventTypes)
                .put("statements", statements)
                .put("resultStatement", resultStatement)
                .put("resultEventType", resultEventType)
                .put("virtualSensorId", virtualSensorId);
        final JsonObject command = createCommand("registerVirtualSensor", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }

    public JsonObject createUpdateVirtualSensorTargetsCommand(String resultStatementName, String targets){
        JsonObject args = new JsonObject()
                .put("resultStatementName", resultStatementName)
                .put("targets", targets);
        final JsonObject command = createCommand("updateVirtualSensorTargets", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }

    public JsonObject createUnregisterVirtualSensorCommands(String virtualSensorId) {
        JsonObject args = new JsonObject()
                .put("virtualSensorId", virtualSensorId);
        final JsonObject command = createCommand("unregisterVirtualSensor", args, UPModuleType.EventProcessor);
        return createCommandsForModuleType(command);
    }

    public JsonObject createRegisterUpdateListenerCommands(String listenerId, String statementName, UpdateListenerTO listener,
            String vertxAddress) {

        JsonObject cepArgs = new JsonObject();
        cepArgs.put("id", listenerId);
        cepArgs.put("statementName", statementName);
        cepArgs.put("vertxAddress", vertxAddress);
        JsonObject cepCommand = createCommand("registerUpdateListener", cepArgs, UPModuleType.EventProcessor);

        JsonObject outboundArgs = outboundSetup.createOutboundListenerConfig(listener);
        JsonObject outboundCommand = createCommand("registerUpdateListener", outboundArgs, UPModuleType.OutboundInterface);

        JsonArray moduleTypeCommands = new JsonArray();
        moduleTypeCommands.add(cepCommand);
        moduleTypeCommands.add(outboundCommand);

        JsonObject commands = new JsonObject();

        commands.put("moduleTypeCommands", moduleTypeCommands);
        return commands;
    }

    public JsonObject createUnregisterUpdateListenerCommands(String listenerId, String statementName) {
        JsonObject args = new JsonObject();
        args.put("id", listenerId);
        args.put("statementName", statementName);
        JsonObject cepCommand = createCommand("unregisterUpdateListener", args, UPModuleType.EventProcessor);
        JsonObject outboundCommand = createCommand("unregisterUpdateListener", args.copy(), UPModuleType.OutboundInterface);

        JsonArray moduleTypeCommands = new JsonArray();
        moduleTypeCommands.add(cepCommand);
        moduleTypeCommands.add(outboundCommand);

        JsonObject commands = new JsonObject();

        commands.put("moduleTypeCommands", moduleTypeCommands);
        return commands;
    }

    public JsonObject createCountProcessedEventsCommand() {
        return createCommand("countProcessedEvents", new JsonObject(), UPModuleType.EventProcessor);
    }
}
