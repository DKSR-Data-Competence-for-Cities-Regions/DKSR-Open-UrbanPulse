package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.TransactionDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ResetModulesFacade {

    private static final Logger LOGGER = Logger.getLogger(ResetModulesFacade.class.getName());

    @EJB
    private UPModuleDAO moduleDAO;

    @EJB
    private TransactionDAO transactionDAO;

    @EJB
    private InboundSetupDAO inboundSetupDAO;

    @EJB
    private PersistenceV3SetupDAO persistenceV3SetupDAO;




    public boolean resetModule(String moduleId) {

        UPModuleEntity module = moduleDAO.queryById(moduleId);

        if (module == null) {
            String sanitizedModuleId = moduleId.replaceAll("[\n|\r|\t]", "_");
            LOGGER.log(Level.INFO, "Module {0} not found", sanitizedModuleId);
            return false;
        } else {
            LOGGER.log(Level.INFO, "Resetting {0} module with ID {1}", new Object[]{module.getModuleType(), module.getId()});
        }

        UPModuleType moduleType = UPModuleType.valueOf(module.getModuleType());
        switch (moduleType) {
            case InboundInterface:
                inboundSetupDAO.unassign(moduleId);
                break;
            case PersistenceV3:
                persistenceV3SetupDAO.unassign(moduleId);
                break;
            case EventProcessor:
            case OutboundInterface:
            case WellKnownNode:
                //Nothing to do here!
                break;

        }

        transactionDAO.deleteById(moduleId);
        moduleDAO.deleteById(moduleId);
        return true;
    }
}
