package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.PersistenceV3SetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.UserAuthCreator;
import io.vertx.core.json.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class PersistenceV3SetupDAO extends AbstractSetupDAO<PersistenceV3SetupEntity> {

    private static final Logger LOG = Logger.getLogger(PersistenceV3SetupDAO.class.getName());

    @Inject
    private UserAuthCreator userAuthCreator;

    @Override
    protected Class<PersistenceV3SetupEntity> getClazz() {
        return PersistenceV3SetupEntity.class;
    }

    /**
     *
     * @param module
     * @param setup
     * @return the setup
     * @deprecated
     */
    @Deprecated
    public JsonObject createPersistenceV3Setup(UPModuleEntity module, JsonObject setup) {
        return this.createModuleSetup(module, setup);
    }

    @Override
    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {
        LOG.info(module.getId());

        try {
            PersistenceV3SetupEntity selectedConfig = this.getAndAssignConfig(module.getId());
            if (selectedConfig == null) {
                LOG.log(Level.INFO, "No config available for {0}", module.getId());
                return new JsonObject();
            } else {
                JsonObject userAuth = userAuthCreator.createUserAuth();
                // Sending credentials over the vert.x eventbus is problematic in a VLAN cluster
                // (only the commercial Hazelcast version does support encryption)
                setup.put("userAuth", userAuth);
                setup.put("storageConfig", new JsonObject(selectedConfig.getSetupJson()));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception getting config", e);
            return new JsonObject();
        }

        return setup;
    }

    @Override
    public UPModuleType getModuleType() {
        return UPModuleType.PersistenceV3;
    }
}
