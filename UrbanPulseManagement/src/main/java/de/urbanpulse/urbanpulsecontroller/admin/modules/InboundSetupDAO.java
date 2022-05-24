package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.ConnectorManagementDAO;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.InboundSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.SensorEventTypesMapper;
import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.List;
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
public class InboundSetupDAO extends AbstractSetupDAO<InboundSetupEntity> {

    @EJB
    private ConnectorManagementDAO connectorDAO;

    @EJB
    private SensorEventTypesMapper sensorEventTypesMapper;

    private static final Logger LOG = Logger.getLogger(InboundSetupDAO.class.getName());

    @Override
    protected Class<InboundSetupEntity> getClazz() {
        return InboundSetupEntity.class;
    }

    /**
     *
     * @param module
     * @param setup
     * @return the setup
     * @deprecated
     */
    @Deprecated
    public JsonObject createInboundSetup(UPModuleEntity module, JsonObject setup) {
        return createModuleSetup(module, setup);
    }

    @Override
    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {
        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        LOG.info("===>>> createInboundSetup START");
        LOG.info(module.getId());

        JsonObject connectorAuth = new JsonObject();
        List<ConnectorEntity> connectors = connectorDAO.queryAll();
        LOG.log(Level.INFO, "===>>> createInboundSetup GOT CONNECTORS, elapsed: {0}", ZonedDateTime.now().toInstant().toEpochMilli() - start);
        connectors.forEach(connectorEntities -> {
            final String connectorId = "" + connectorEntities.getId();
            final String hmacKey = connectorEntities.getKey();
            connectorAuth.put(connectorId, hmacKey);
        });
        setup.put("connectorAuth", connectorAuth);
        LOG.log(Level.INFO, "===>>> createInboundSetup BUILT AUTH, elapsed: {0}", ZonedDateTime.now().toInstant().toEpochMilli() - start);

        JsonObject sensorEventTypes = sensorEventTypesMapper.readSensorEventTypes();
        LOG.log(Level.INFO, "===>>> createInboundSetup READ SENSOR EVENTTYPES, elapsed: {0}", ZonedDateTime.now().toInstant().toEpochMilli() - start);
        setup.put("sensorEventTypes", sensorEventTypes);

        try {
            InboundSetupEntity selectedConfig = this.getAndAssignConfig(module.getId());
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

        LOG.log(Level.INFO, "===>>> createInboundSetup END, elapsed: {0}", ZonedDateTime.now().toInstant().toEpochMilli() - start);
        return setup;
    }

    @Override
    public UPModuleType getModuleType() {
        return UPModuleType.InboundInterface;
    }

}
