package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.ModuleUpdateManager;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
public class SensorModuleUpdateWrapper {

    @Inject
    private CommandsJsonFactory commandsJsonFactory;

    @Inject
    private ModuleUpdateManager moduleUpdateManager;

    public void updateConnector(String connectorId, String authKey) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createUpdateConnectorCommands(connectorId, authKey);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("update connector failed", errorList);
        }
    }

    public void registerSensor(String sensorId, String eventTypeName, String connectorId) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createRegisterSensorCommands(sensorId, eventTypeName, connectorId);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("register sensor failed", errorList);
        }
    }

    public void updateSensor(String sensorId, String eventTypeName) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createUpdateSensorCommands(sensorId, eventTypeName);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("update sensor failed", errorList);
        }
    }

    public void unregisterSensor(String sensorId) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterSensorCommands(sensorId);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("unregister sensor failed", errorList);
        }
    }

    public void unregisterConnector(String connectorId) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createUnregisterConnectorCommands(connectorId);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("unregister connector failed", errorList);
        }
    }

    public void registerConnector(String connectorId, String authKey) throws SensorModuleUpdateWrapperException {
        JsonObject commands = commandsJsonFactory.createRegisterConnectorCommands(connectorId, authKey);
        List<JsonObject> errorList = moduleUpdateManager.runModuleTypeCommands(commands);
        if (!isSuccessful(errorList)) {
            throw new SensorModuleUpdateWrapperException("register connector failed", errorList);
        }
    }

    public boolean isSuccessful(List<JsonObject> errorList) {
        return errorList.stream().allMatch(Objects::isNull);
    }
}
