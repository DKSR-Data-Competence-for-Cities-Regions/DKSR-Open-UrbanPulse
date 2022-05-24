package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.AbstractSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.EventProcessorSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.HeartbeatHandler;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.SetupMasterConnector;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * REST Web Service for system health and performance KPIs (=key performance indicators)
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class KpiRestService extends AbstractRestService {

    @Inject
    private UPModuleDAO moduleDAO;

    @Inject
    private HeartbeatHandler heartbeatHandler;

    @Inject
    private SetupMasterConnector setupMasterConnector;

    @Inject
    private EventProcessorSetupDAO eventProcessorSetupDAO;


    @Inject
    private InboundSetupDAO inboundSetupDAO;

    @Inject
    private OutboundSetupDAO outboundSetupDAO;

    @Inject
    private PersistenceV3SetupDAO persistenceV3SetupDAO;



    public Response getKpiOverview(Integer refresh) {
        JsonObject result = new JsonObject();
        DateTime now = DateTime.now(DateTimeZone.UTC);

        String timestamp = now.toString();
        result.put("timestamp", timestamp);

        int heartbeatInterval = heartbeatHandler.getTimeout();
        result.put("heartbeatInterval", heartbeatInterval);

        final Set<UPModuleType> availableSetups = setupMasterConnector.getAvailableSetups();

        Map<UPModuleType, Integer> availableSetupsCount = new TreeMap<>();
        availableSetups.forEach(setupType -> availableSetupsCount.put(setupType, determineAvailableConfiguration(setupType)));

        final JsonArray availableSetupsJson = new JsonArray();
        availableSetupsCount.forEach((t, c) -> availableSetupsJson.add(new JsonObject().put("name", t).put("count", c)));
        result.put("availableSetups", availableSetupsJson);

        JsonArray registeredModules = new JsonArray();
        List<UPModuleEntity> modules = moduleDAO.queryAll();
        for (UPModuleEntity module : modules) {
            JsonObject moduleInfo = new JsonObject();

            String moduleId = module.getId();
            moduleInfo.put("moduleId", moduleId);

            String moduleType = module.getModuleType();
            moduleInfo.put("moduleType", moduleType);

            String moduleState = module.getModuleState().toString();
            moduleInfo.put("moduleState", moduleState);

            Date lastHeartbeatDate = module.getLastHeartbeat();
            String lastHeartbeat = null;
            if (lastHeartbeatDate != null) {
                DateTime lastHeartbeatDateTime = new DateTime(lastHeartbeatDate.getTime()).withZone(DateTimeZone.UTC);
                long millisUntilNextHeartbeat = (long) heartbeatInterval + lastHeartbeatDate.getTime() - now.getMillis();
                long secondsUntilNextHeartbeat = millisUntilNextHeartbeat / 1000L;
                moduleInfo.put("secondsUntilNextHeartbeat", secondsUntilNextHeartbeat);

                lastHeartbeat = lastHeartbeatDateTime.toString();
                long millisSinceLastHeartbeat = now.getMillis() - lastHeartbeatDate.getTime();
                long secondsSinceLastHeartbeart = millisSinceLastHeartbeat / 1000L;
                moduleInfo.put("secondsSinceLastHeartbeat", secondsSinceLastHeartbeart);
            }

            moduleInfo.put("lastHeartbeat", lastHeartbeat);
            registeredModules.add(moduleInfo);
        }

        result.put("registeredModules", registeredModules);

        Response.ResponseBuilder builder = Response.ok(result.encodePrettily());
        if (refresh != null) {
            builder.header("Refresh", refresh);
        }
        return builder.build();
    }

    /**
     *
     * @param setupType Any module type
     * @return number of configurations for this module in the database, 0 for WellKnownNode and 1 for modules without configurations in the database
     */
    private Integer determineAvailableConfiguration(UPModuleType setupType) {
        AbstractSetupDAO currentDAO;
        switch (setupType) {
            case WellKnownNode:
                return 0;
            case EventProcessor:
            case OutboundInterface:
                return 1;
            case InboundInterface:
                currentDAO = inboundSetupDAO;
                break;
            case PersistenceV3:
                currentDAO = persistenceV3SetupDAO;
                break;
            default:
                throw new AssertionError("There seem to exist modules that are unknown to the KpiRestService: " + setupType.name());
        }
        return currentDAO.queryAll().size();
    }
}
