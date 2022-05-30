package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.util.status.UPModuleState;
import io.vertx.core.json.JsonObject;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Lock(LockType.READ)
public class HeartbeatHandler {

    @EJB
    private UPModuleDAO moduleDAO;

    @Resource
    private TimerService timerService;

    @EJB
    private ResetModulesFacade resetModulesFacade;

    private static final Logger LOGGER = Logger.getLogger(HeartbeatHandler.class.getName());

    @PostConstruct
    public void init() {
        int duration = getTimeout();
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(0, duration, timerConfig);
        Date now = new Date();
        for (UPModuleEntity module : moduleDAO.queryAll()) {
            module.setLastHeartbeat(now);
            moduleDAO.merge(module);
        }
    }

    @Timeout
    public void checkModules() {
        try {
            for (UPModuleEntity module : moduleDAO.queryAll()) {
                checkModule(module);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "checkModules: {0}", ex.getMessage());
        }
    }

    public void receivedHeartbeatForModule(String moduleId, UPModuleState state) {
        this.receivedHeartbeatForModule(moduleId, state, Optional.empty());
    }

    public void receivedHeartbeatForModule(String moduleId, UPModuleState state, Optional<CommandResult> callback) {
        UPModuleEntity module = moduleDAO.queryById(moduleId);
        if (module == null) {
            LOGGER.log(Level.SEVERE, "got heartbeat for unknown module with id [{0}]", moduleId);
            String subject = "Heartbeat";
            String body = moduleId + ": heartbeat for unknown module";

            // heartbeat received from module that is no longer known (e.g. cleared because of a too old heartbeat earlier)
            callback.ifPresent(c -> c.done(new JsonObject().put("ERROR", "Unknown module!"), null));
        } else {

            module.setLastHeartbeat(new Date());
            module.setModuleState(state);
            moduleDAO.merge(module);
            callback.ifPresent(c -> c.done(new JsonObject(), null));
        }

    }

    public int getTimeout() {
        return 60000;
    }

    private void checkModule(UPModuleEntity module) {
        long now = System.currentTimeMillis();

        Date heartbeat = module.getLastHeartbeat();
        if ((heartbeat == null) || (now - heartbeat.getTime() > getTimeout())) {
            LOGGER.log(Level.INFO, "heartbeat of {0} module with ID {1} is too old", new Object[]{module.getModuleType(), module.getId()});
            clearModule(module);
        }
    }

    private void clearModule(UPModuleEntity module) {
        resetModulesFacade.resetModule(module.getId());
    }

}
