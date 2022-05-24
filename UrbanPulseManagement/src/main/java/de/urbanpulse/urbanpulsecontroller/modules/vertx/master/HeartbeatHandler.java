package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.util.EmailSender;
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
import javax.inject.Inject;
import javax.mail.Session;

/**
 *
 *
 * Create JavaMail-Session with admin console
 *
 * JNDI-Name HeartbeatEMail
 *
 * add properties *
 *
 * mail.smtp.startttls.enable true *
 *
 * mail.smtp.auth true *
 *
 * mail.smtp.user &lt;mail user&gt;*
 *
 * mail.smtp.password &lt;password of mail user&gt; *
 *
 * mail.to recipient for heartbeat mails *
 *
 * mail.debug true (mail delivery is disabled) *
 *
 * mail.heartbeat.timeout timeout of modules to send heartbeats (n msec)
 *
 * hint: *
 *
 * if mail.debug = true *
 *
 * no other properties are needed
 *
 *
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

    @Inject
    private EmailSender emailSender;

    @Resource(lookup = "HeartbeatEMail")
    private Session mailSession;

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
            emailSender.sendEmail(mailSession, subject, body);
            // heartbeat received from module that is no longer known (e.g. cleared because of a too old heartbeat earlier)
            callback.ifPresent(c -> c.done(new JsonObject().put("ERROR", "Unknown module!"), null));
        } else {
            if (module.isMailSent()) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "got heartbeat for module with id [{0}]", moduleId);
                String subject = "Heartbeat";
                String body = moduleId + ": heartbeat received after error";
                emailSender.sendEmail(mailSession, subject, body);
            }
            module.setLastHeartbeat(new Date());
            module.setMailSent(false);
            module.setModuleState(state);
            moduleDAO.merge(module);
            callback.ifPresent(c -> c.done(new JsonObject(), null));
        }

    }

    public int getTimeout() {
        try {
            return Integer.parseInt(mailSession.getProperty("mail.heartbeat.timeout"));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "cannot parse value for mail.heartbeat.timeout. Using default");
            return 60000;
        }
    }

    private void checkModule(UPModuleEntity module) {
        long now = System.currentTimeMillis();

        Date heartbeat = module.getLastHeartbeat();
        if ((heartbeat == null) || (now - heartbeat.getTime() > getTimeout())) {
            LOGGER.log(Level.INFO, "heartbeat of {0} module with ID {1} is too old", new Object[]{module.getModuleType(), module.getId()});
            if (!module.isMailSent()) {
                String subject = "Heartbeat";
                String body;
                if (heartbeat == null) {
                    body = module.getId() + ": no heartbeat";
                } else {
                    body = module.getId() + ": no heartbeat for " + ((now - heartbeat.getTime()) / 1000) + " seconds";
                }
                body += " (" + module.getModuleType() + ")";
                if (emailSender.sendEmail(mailSession, subject, body)) {
                    module.setMailSent(true);
                }
            }
            clearModule(module);
        }
    }

    private void clearModule(UPModuleEntity module) {
        resetModulesFacade.resetModule(module.getId());
    }

}
