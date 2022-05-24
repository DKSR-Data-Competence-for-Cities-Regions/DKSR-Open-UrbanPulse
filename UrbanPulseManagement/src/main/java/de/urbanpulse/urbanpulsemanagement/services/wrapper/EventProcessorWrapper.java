package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.ModuleUpdateManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * wraps what used to be more or less the EventProcessorRuntimeLocal interface,
 * delegating calls to vert.x modules in the cluster via the JCA vert.x resource
 * adapter
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class EventProcessorWrapper {

    @Inject
    private CommandsJsonFactory commandsJsonFactory;

    @Inject
    private ModuleUpdateManager moduleUpdateManager;

    @Inject
    private OutboundInterfacesManagementDAO outboundInterfacesManagementDAO;

    public void unregisterEventType(String name) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterEventTypeCommands(name);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands); //Not called when on DELETE Virtual Sensor.
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("unregisterEventType failed", errorList);
        }
    }

    public void registerEventType(String name, javax.json.JsonObject config) throws EventProcessorWrapperException {
        JsonObject tmpConfig = new JsonObject(config.toString());
        JsonObject commands = commandsJsonFactory.createRegisterEventTypeCommands(name, tmpConfig);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("registerEventType failed", errorList);
        }
    }

    /**
     * @return number of processed events / null in case of error
     * @throws EventProcessorWrapperException a command could not be ran
     */
    public Long countProcessedEvents() throws EventProcessorWrapperException {
        JsonObject command = commandsJsonFactory.createCountProcessedEventsCommand();
        try {
            JsonObject result = moduleUpdateManager.runSingleInstanceReturnCommand(command, new CountProcessedEventsResultVerifier());
            if (result == null) {
                throw new EventProcessorWrapperException("countProcessedEvents failed");
            } else {
                long processedEvents = result.getJsonObject("body").getLong("processedEvents");
                return processedEvents;
            }
        } catch (Exception e) {
            throw new EventProcessorWrapperException("Unable to retrieve processed events", e);
        }
    }

    public void registerStatement(String query, String name) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createRegisterStatementCommands(name, query);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("registerStatement failed", errorList);
        }
    }

    public void unregisterStatement(String name) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterStatementCommands(name);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("unregisterStatement failed", errorList);
        }
    }

    public void registerUpdateListener(StatementTO statement, UpdateListenerTO listener) throws EventProcessorWrapperException {
        registerUpdateListener(statement.getName(), listener.getId());
    }

    @Deprecated
    public void registerUpdateListener(String statementName, String listenerId) throws EventProcessorWrapperException {
        UpdateListenerTO listener = outboundInterfacesManagementDAO.getUpdateListenerById(listenerId);

        String vertxAddress = OutboundSetupDAO.OUTBOUND_VERTX_ADDRESS;

        JsonObject commands = commandsJsonFactory.createRegisterUpdateListenerCommands(listenerId, statementName, listener, vertxAddress);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("registerUpdateListener failed", errorList);
        }
    }

    public void unregisterUpdateListener(String listenerId, String statementName) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterUpdateListenerCommands(listenerId, statementName);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("unregisterUpdateListener failed", errorList);
        }
    }

    public void registerVirtualSensor(String virtualSensorId, JsonArray eventTypes, JsonArray statements,
            JsonObject resultStatement, JsonObject resultEventType) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createRegisterVirtualSensorCommands(virtualSensorId, eventTypes, statements, resultStatement, resultEventType);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("registerVirtualSensor failed", errorList);
        }
    }

    public void updateVirtualSensorTargets(String resultStatementName, String targets) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createUpdateVirtualSensorTargetsCommand(resultStatementName, targets);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException(" failed to update targets array of the virtual sensor", errorList);
        }
    }

    public void unregisterVirtualSensor(String virtualSensorId) throws EventProcessorWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterVirtualSensorCommands(virtualSensorId);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new EventProcessorWrapperException("unregisterVirtualSensor failed", errorList);
        }
    }

    public boolean isSuccessful(List<JsonObject> errorList) {
        return errorList.stream().allMatch(Objects::isNull);
    }
}
