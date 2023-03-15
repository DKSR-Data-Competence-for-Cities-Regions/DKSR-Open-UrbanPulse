package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.ConnectionHandler;
import de.urbanpulse.transfer.TransactionManager;
import de.urbanpulse.transfer.TransferStructureFactory;
import de.urbanpulse.transfer.TransportLayer;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.AbstractModuleSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.AbstractSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.BackchannelSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.EventProcessorSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.ModuleSetup;
import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.TransactionDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.util.status.UPModuleState;
import io.vertx.core.json.JsonObject;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * listens to incoming vert.x messages from remote modules
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Startup
@Singleton
@LocalBean
public class SetupMasterConnector {

    private static final Logger LOGGER = Logger.getLogger(SetupMasterConnector.class.getName());
    public static final String SETUP_MASTER_ADDRESS = "sm_address";
    @EJB
    private UPModuleDAO moduleDAO;
    @EJB
    private TransactionDAO transactionDAO;
    @EJB
    private InboundSetupDAO inboundSetupDAO;
    @EJB
    private HeartbeatHandler heartbeatHandler;
    @EJB
    private PersistenceV3SetupDAO persistenceV3SetupDAO;
    @EJB
    private EventProcessorSetupDAO eventProcessorSetupDAO;
    @EJB
    private OutboundSetupDAO outboundSetupDAO;
    @EJB
    private BackchannelSetupDAO backchannelSetupDAO;
    @EJB
    private ResetModulesFacade resetModulesFacade;
    private ConnectionHandler connectionHandler;

    private final Map<UPModuleType, ModuleSetup> availableModules = new EnumMap<>(UPModuleType.class);

    @Inject
    @VertxEmbedded
    private TransportLayer transport;

    @PostConstruct
    private void init() {
        try {
            this.connectionHandler = new ConnectionHandler();

            this.connectionHandler.setTransport(transport);
            this.connectionHandler.setTransactionManager(new TransactionManager());
            this.addModuleSetup(inboundSetupDAO)
                    .addModuleSetup(persistenceV3SetupDAO)
                    .addModuleSetup(eventProcessorSetupDAO)
                    .addModuleSetup(outboundSetupDAO)
                    .addModuleSetup(backchannelSetupDAO);
            this.connectionHandler.setCommandHandler(this);
            this.connectionHandler.setConnectionId(SETUP_MASTER_ADDRESS, null);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "failed to access vertx connection");
            throw new IllegalStateException(ex);
        }
    }

    /**
     * clear all module transactions / connections / registrations, then
     * broadcast on the "module_reset" vertx address, every module registers a
     * handler on this address and resets its own state
     */
    public void reset() {
        LOGGER.log(Level.INFO, "++++++++++++++++++  resetting vertx modules +++++++++++++++ ");
        this.transactionDAO.deleteAll();
        this.inboundSetupDAO.clearAssignments();
        this.persistenceV3SetupDAO.clearAssignments();
        this.backchannelSetupDAO.clearAssignments();
        this.moduleDAO.deleteAll();
        this.transport.publish("module_reset", new JsonObject());
        LOGGER.log(Level.INFO, "++++++++++++++++++  reset done +++++++++++++++ ");
    }

    //SetupMaster
    public SetupMasterConnector addModuleSetup(ModuleSetup moduleSetup) {
        this.availableModules.put(moduleSetup.getModuleType(), moduleSetup);
        return this;
    }

    /**
     * "register" command implementation - registers a module
     *
     * @param args arguments for the registration containing the moduleType
     * @param createUndoCommand unused parameter, required for consistent
     * command method signature
     * @param callback the callback to be called when completed
     */
    public void register(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        Long start = System.currentTimeMillis();
        String moduleType = (String) args.get("moduleType");
        LOGGER.log(Level.SEVERE, "Start registration of module of type {0}", moduleType);
        UPModuleEntity module = moduleDAO.create(moduleType);
        final String id = "" + module.getId();
        JsonObject result = new JsonObject();
        result.put("id", id);
        LOGGER.log(Level.INFO, " registered module of type [{0}] with id[{1}]", new Object[]{moduleType, id});
        // avoid timeout before first heartbeat signal
        module.setLastHeartbeat(new Date());
        module.setModuleState(UPModuleState.UNKNOWN);
        module.setMailSent(false);

        LOGGER.log(Level.SEVERE, "Registration of {0} took : {1} ms", new Object[]{moduleType, System.currentTimeMillis() - start});
        callback.done(result, null);
    }

    /**
     * "unregister" command implementation - unregisters a module
     *
     * @param args arguments containing the id for the unregistration
     * @param createUndoCommand unused parameter, required for consistent
     * command method signature
     * @param callback the callback to be called when completed
     */
    public void unregister(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        JsonObject result = null;
        String id = (String) args.get("id");
        LOGGER.log(Level.INFO, "SetupMaster is trying to delete module {0}", id);
        if (moduleDAO.deleteById(id) == null) {
            result = new JsonObject();
            result.put("error", "unknown id");
            result.put("id", id);
            LOGGER.log(Level.INFO, "could not find module registration with id [{0}]", id);
        } else {
            LOGGER.log(Level.INFO, "unregistered module with id[{0}]", id);
        }
        callback.done(result, null);
    }

    /**
     * @return a list of module-types for which a setup exists
     */
    public Set<UPModuleType> getAvailableSetups() {
        return availableModules.keySet();
    }

    /**
     * "heartbeat" command implementation - handle heartbeat of a module
     *
     * @param args the arguments containing the moduleId
     * @param createUndoCommand unused parameter, required for consistent
     * command method signature
     * @param callback the callback to be called when completed
     */
    public void heartbeat(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        String moduleId = (String) args.get("id");
        UPModuleState state = UPModuleState.valueOf((String) args.get("state"));
        heartbeatHandler.receivedHeartbeatForModule(moduleId, state, Optional.of(callback));
    }

    /**
     * "sendSetup" command implementation - sends the initial setup to a module
     *
     * @param args the arguments containing the moduleId
     * @param createUndoCommand unused parameter, required for consistent
     * command method signature
     * @param callback the callback to be called when completed
     */
    public void sendSetup(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        String moduleId = (String) args.get("id");
        UPModuleEntity module = moduleDAO.queryById(moduleId);
        if (module == null) {
            LOGGER.log(Level.INFO, "sendSetup() received from unknown module {0}", moduleId);
            // sendSetup received from module that is no longer known (e.g. cleared because of a too old heartbeat earlier)
            callback.done(new JsonObject(), null);
            connectionHandler.sendResetConnection(moduleId, 5000,
                    (JsonObject event) -> LOGGER.log(Level.INFO, "........... resetConnection result: {0}", event));
            return;
        }
        heartbeatHandler.receivedHeartbeatForModule(moduleId, UPModuleState.UNKNOWN);
        UPModuleType moduleType = UPModuleType.valueOf(module.getModuleType());
        if (availableModules.containsKey(moduleType)) {
            cleanAnyConfigsUsedByNonExistingModules(getSetupDaoForModuleType(moduleType));
            callback.done(availableModules.get(moduleType).createModuleSetup(module, createCommonSetup()), null);
        } else {
            if (moduleType == UPModuleType.WellKnownNode) {
                callback.done(createCommonSetup(), null);
            } else {
                JsonObject error = new JsonObject();
                error.put("error", "unsupported moduleType [" + moduleType + "]");
                callback.done(error, null);
            }
            heartbeatHandler.receivedHeartbeatForModule(moduleId, UPModuleState.UNKNOWN);
        }
    }

    private JsonObject createCommonSetup() {
        JsonObject setup = new JsonObject();
        int timeout = heartbeatHandler.getTimeout() / 2;
        timeout = (timeout > 1) ? (timeout - 1) : 0;
        setup.put(TransferStructureFactory.COMMAND_HEARTBEAT, timeout);
        return setup;
    }

    private void cleanAnyConfigsUsedByNonExistingModules(AbstractSetupDAO<AbstractModuleSetupEntity> setupDAOForModule) {
        if (setupDAOForModule != null) {
            List<AbstractModuleSetupEntity> setupEntitysList = setupDAOForModule.queryAll();
            for (AbstractModuleSetupEntity setupEntity : setupEntitysList) {
                String setupModuleId = setupEntity.getModuleId();
                if (setupModuleId != null && !setupModuleId.trim().isEmpty() && moduleDAO.queryById(setupModuleId) == null) {
                    //a module that cannot be found in up_modules should not use a config
                    LOGGER.log(Level.INFO, "A module with id {0} is not part of up_modules, but is blocking a config! "
                            + "The module will be removed!", setupModuleId);
                    //clean the id and "unlock" the config
                    setupDAOForModule.unassign(setupModuleId);
                }
            }
        }
    }

    //Unfortunately using the ModuleSetup-interface does not supply us with
    //the functionality of loading the configs for a module from the database.
    //That is why we need the specific AbstractSetupDAO.
    private AbstractSetupDAO<AbstractModuleSetupEntity> getSetupDaoForModuleType(UPModuleType moduleType) {
        AbstractSetupDAO<AbstractModuleSetupEntity> setupDAOForModule = null;

        switch (moduleType) {
            case PersistenceV3:
                setupDAOForModule = (AbstractSetupDAO)persistenceV3SetupDAO;
                break;
            case InboundInterface:
                setupDAOForModule = (AbstractSetupDAO)inboundSetupDAO;
                break;
            case Backchannel:
                setupDAOForModule = (AbstractSetupDAO)backchannelSetupDAO;
                break;
            //Outbound and CEP are not of type AbstractSetupDAO
            //but we dont want to log a warning when these methods are
            //given as input. Such warning can have a confusing effect.
            case EventProcessor:
                break;
            case OutboundInterface:
                break;
            default:
                LOGGER.log(Level.WARNING, "Can not get the setupDao for module {0}", moduleType.name());
                break;
        }

        return setupDAOForModule;
    }

    //SetupMaster
    @PreDestroy
    private void preDestroy() {
        try {
            this.transport.unregisterHandlers();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * ResetCommand will not be send here
     *
     * @param moduleId the module to be reseted
     * @return true if module was found and cleared from the database
     */
    public boolean reset(String moduleId) {
        return resetModulesFacade.resetModule(moduleId);
    }
}
