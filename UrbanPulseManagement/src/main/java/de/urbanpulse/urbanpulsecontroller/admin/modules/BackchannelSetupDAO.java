package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.ConnectorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.BackchannelSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class BackchannelSetupDAO extends AbstractSetupDAO<BackchannelSetupEntity> {

    @EJB
    private ConnectorManagementDAO connectorDAO;

    private static final Logger LOG = Logger.getLogger(BackchannelSetupDAO.class.getName());

    @Override
    protected Class<BackchannelSetupEntity> getClazz() {
        return BackchannelSetupEntity.class;
    }

    @Override
    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {
        long startMilli = Instant.now().toEpochMilli();

        LOG.info("Creating Backchannel setup");
        LOG.info(module.getId());

        List<ConnectorEntity> connectors = connectorDAO.queryAll();
        LOG.log(Level.FINER, "Backchannel setup: GOT CONNECTORS in {0} msec", (Instant.now().toEpochMilli() - startMilli));

        JsonObject connectorsObj = new JsonObject();

        connectors.stream().forEach(connector -> {
            final String connectorId = "" + connector.getId();

            JsonObject connectorObj = new JsonObject();
            connectorObj.put("hmacKey", connector.getKey());
            connectorObj.put("backchannelKey", connector.getBackchannelKey());
            connectorObj.put("backchannelEndpoint", connector.getBackchannelEndpoint());

            final List<String> completeSidList = connector.getSensors().stream().map(SensorEntity::getId).map(String::valueOf).collect(Collectors.toList());
            connectorObj.put("sidList", completeSidList);

            connectorsObj.put(connectorId, connectorObj);
        });

        setup.put("connectors", connectorsObj);

        try {
            BackchannelSetupEntity selectedConfig = this.getAndAssignConfig(module.getId());
            if (selectedConfig == null) {
                LOG.log(Level.INFO, "No config available for {0}", module.getId());
                return new JsonObject();
            } else {
                setup = setup.mergeIn(new JsonObject(selectedConfig.getSetupJson()));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception getting config", e);
            return new JsonObject();
        }

        LOG.log(Level.INFO, "Creating Backchannel setup finished in {0} msec", Instant.now().toEpochMilli() - startMilli);
        return setup;
    }

    @Override
    public UPModuleType getModuleType() {
        return UPModuleType.Backchannel;
    }
}
