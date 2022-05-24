package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.Command;
import de.urbanpulse.transfer.TransportLayer;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.CommandResultVerifier;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * dispatches (mainly update-)commands to connected vert.x modules
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ModuleUpdateManager {

    private static final Logger LOGGER = Logger.getLogger(ModuleUpdateManager.class.getName());

    @EJB
    private UPModuleDAO moduleDAO;

    @EJB
    private ModuleTransactionExecutor transactionExecutor;

    @Inject
    @VertxEmbedded
    private TransportLayer transport;

    @EJB
    private ResetModulesFacade resetModulesFacade;

    @PreDestroy
    private void preDestroy() {
        try {
            this.transport.unregisterHandlers();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param commandJson command json containing "method" String and "args"
     * JsonObject
     * @param verifier verifying the command object
     * @return result JsonObject / null in case of error
     */
    public JsonObject runSingleInstanceReturnCommand(JsonObject commandJson, CommandResultVerifier verifier) throws UPXAException {
        String moduleTypeName = commandJson.getString("moduleType");
        List<UPModuleEntity> modules = moduleDAO.queryFilteredBy("moduleType", moduleTypeName);
        if (modules.size() != 1) {
            Logger.getLogger(ModuleTransactionExecutor.class.getName()).log(Level.SEVERE, "can run single return command only for single module instance, type {0} has {1}", new Object[]{moduleTypeName, modules.size()});
            return null;
        }

        UPModuleEntity module = modules.get(0);
        String moduleId = module.getId();
        ConnectionHandlerJEE connectionHandler = new ConnectionHandlerJEE(moduleId, transport);
        return executeSingleInstanceReturnCommand(commandJson, connectionHandler,
                TransactionalCommandTask.DEFAULT_TIMEOUT, verifier);
    }

    /**
     * @param moduleId id of module
     * @param commandJson command json containing "method" String and "args"
     * JsonObject
     * @param verifier verifying the command object
     * @return result JsonObject / null in case of error
     */
    public JsonObject runSingleInstanceReturnCommand(String moduleId, JsonObject commandJson, CommandResultVerifier verifier) throws UPXAException {
        ConnectionHandlerJEE connectionHandler = new ConnectionHandlerJEE(moduleId, transport);
        return executeSingleInstanceReturnCommand(commandJson, connectionHandler,
                TransactionalCommandTask.DEFAULT_TIMEOUT, verifier);
    }

    /**
     * @param commandsJson the command in json format
     *
     * @return a list of errors encountered during execution of the commands or
     * null if the specific command was successful
     */
    public List<JsonObject> runModuleInstanceCommands(JsonObject commandsJson) {
        List<TransactionalCommandTask> tasks = new LinkedList<>();

        JsonArray moduleCommands = commandsJson.getJsonArray("moduleInstanceCommands");

        for (Object obj : moduleCommands) {
            JsonObject moduleCommand = (JsonObject) obj;
            String moduleId = moduleCommand.getString("moduleId");
            Command command = createCommandFromJson(moduleCommand);
            addTaskForModuleInstance(tasks, moduleId, command);
        }

        return launchOrderDependentTasks(tasks, 0);
    }

    /**
     * @param commandsJson the command in json format
     *
     * @return a list of errors encountered during execution of the tasks or
     * null if the specific task was successful
     */
    public List<JsonObject> runModuleTypeCommands(JsonObject commandsJson) {
        List<TransactionalCommandTask> tasks = new LinkedList<>();

        JsonArray moduleCommands = commandsJson.getJsonArray("moduleTypeCommands");

        for (Object obj : moduleCommands) {
            JsonObject moduleCommand = (JsonObject) obj;
            String moduleTypeName = moduleCommand.getString("moduleType");
            UPModuleType moduleType = UPModuleType.valueOf(moduleTypeName);
            Command command = createCommandFromJson(moduleCommand);
            addTasksForAllModuleInstances(tasks, moduleType, command);
        }

        return launchOrderDependentTasks(tasks, 0);
    }

    private List<JsonObject> launchOrderDependentTasks(List<TransactionalCommandTask> tasks, int recursionDepth) {
        if (recursionDepth >= 3) {
            return Collections.emptyList();
        }

        List<String> noHandlerModuleIDs = new LinkedList<>();
        List<JsonObject> successful = transactionExecutor.executeOrderDependentTasks(tasks, noHandlerModuleIDs);
        boolean noHandlerSuccess = handleNoHandlerModuleIDs(noHandlerModuleIDs);

        if (noHandlerSuccess && (!noHandlerModuleIDs.isEmpty())) {
            Logger.getLogger(ModuleUpdateManager.class.getName()).log(Level.WARNING, "No handlers for some modules, retrying after having them removed...");

            List<TransactionalCommandTask> cleanedTasks = new LinkedList<>();
            for (TransactionalCommandTask originalTask : tasks) {
                boolean moduleIDInNoHandler = false;
                for (String moduleID : noHandlerModuleIDs) {
                    moduleIDInNoHandler |= moduleID.equals(originalTask.getModuleId());
                }

                if (!moduleIDInNoHandler) {
                    cleanedTasks.add(originalTask);
                }
            }

            successful = launchOrderDependentTasks(cleanedTasks, recursionDepth + 1);
        }

        return successful;
    }

    boolean handleNoHandlerModuleIDs(List<String> noHandlerModuleIDs) {
        boolean success = true;
        if ((noHandlerModuleIDs == null) || (noHandlerModuleIDs.isEmpty())) {
            return true;
        }

        for (String noHandlerModuleID : noHandlerModuleIDs) {
            LOGGER.log(Level.FINE, "ModuleUpdateManager is trying to delete module {0}", noHandlerModuleID);
            boolean resetSuccess = resetModulesFacade.resetModule(noHandlerModuleID);
            if (!resetSuccess) {
                success = false;
                LOGGER.log(Level.WARNING, "Deleting module {0} failed because it did not exist (anymore)", noHandlerModuleID);
            } else {
                LOGGER.log(Level.INFO, "Success deleting module {0}", noHandlerModuleID);
            }
        }

        return success;
    }

    private Command createCommandFromJson(JsonObject commandJson) {
        String method = commandJson.getString("method");
        JsonObject argsJson = commandJson.getJsonObject("args");

        Map<String, Object> args = argsJson.getMap();
        return new Command(method, args);
    }

    private void addTasksForAllModuleInstances(List<TransactionalCommandTask> tasks, UPModuleType moduleType, Command command) {
        List<UPModuleEntity> modules = moduleDAO.queryFilteredBy("moduleType", moduleType.name());
        for (UPModuleEntity module : modules) {
            String moduleId = module.getId();
            addTaskForModuleInstance(tasks, moduleId, command);
        }
    }

    private void addTaskForModuleInstance(List<TransactionalCommandTask> tasks, String moduleId, Command command) {
        ConnectionHandlerJEE connectionHandler = new ConnectionHandlerJEE(moduleId, transport);
        TransactionalCommandTask task = new TransactionalCommandTask(moduleId, command, connectionHandler);
        tasks.add(task);
    }

    /**
     *
     * @param commandJson the command in json format
     * @param connectionHandler the handler for the communication
     * @param timeout how long will be waited for response
     * @param verifier verifying the command
     * @return the result object returned by the module in case of success, null in case of failure
     * @throws de.urbanpulse.urbanpulsecontroller.modules.vertx.master.UPXAException
     */
    private JsonObject executeSingleInstanceReturnCommand(final JsonObject commandJson,
            final ConnectionHandlerJEE connectionHandler, final long timeout, CommandResultVerifier verifier) throws UPXAException {
        final String method = commandJson.getString("method");
        final Map<String, Object> args = commandJson.getJsonObject("args").getMap();

        JsonObject result = connectionHandler.sendCommand(method, args, timeout);
        if (verifier.verify(result)) {
            return result;
        }

        return null;
    }
}
