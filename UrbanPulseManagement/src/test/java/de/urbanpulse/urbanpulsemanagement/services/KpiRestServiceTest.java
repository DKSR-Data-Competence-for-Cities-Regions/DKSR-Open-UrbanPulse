package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.InboundSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.PersistenceV3SetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.BackchannelSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.EventProcessorSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.HeartbeatHandler;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.SetupMasterConnector;
import de.urbanpulse.util.status.UPModuleState;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class KpiRestServiceTest {

    @Mock
    private UPModuleDAO moduleDAO;

    @Mock
    private HeartbeatHandler heartbeatHandler;

    @Mock
    private SetupMasterConnector setupMasterConnector;

    @Mock
    private EventProcessorSetupDAO eventProcessorSetupDAO;

    @Mock
    private BackchannelSetupDAO backchannelSetupDAO;

    @Mock
    private InboundSetupDAO inboundSetupDAO;

    @Mock
    private OutboundSetupDAO outboundSetupDAO;

    @Mock
    private PersistenceV3SetupDAO persistenceV3SetupDAO;



    @InjectMocks
    KpiRestService kpiRestService = new KpiRestService();

    private final int heartBeatTimeout = 3000;

    private final List<UPModuleEntity> moduleList = new ArrayList();
    private final Set<UPModuleType> availableModules = new HashSet<>();

    @Before
    public void setupTest() {
        Date now = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));

        Mockito.when(heartbeatHandler.getTimeout()).thenReturn(heartBeatTimeout);

        UPModuleType[] moduleTypes = new UPModuleType[UPModuleType.values().length - 1];
        int offset = 0;
        for (int i = 0; i < moduleTypes.length; i++) {
            UPModuleType currentType = UPModuleType.values()[i];
            moduleTypes[i] = UPModuleType.values()[i + offset];
        }
        for (int i = 0; i < moduleTypes.length; i++) {
            final UPModuleEntity upModuleEntity = new UPModuleEntity();
            upModuleEntity.setId(Integer.toString(i));
            upModuleEntity.setLastHeartbeat(now);
            upModuleEntity.setModuleState(UPModuleState.HEALTHY);
            upModuleEntity.setModuleType(moduleTypes[i].name());
            moduleList.add(upModuleEntity);
        }

        Mockito.when(moduleDAO.queryAll()).thenReturn(moduleList);

        availableModules.addAll(Arrays.asList(moduleTypes));
        Mockito.when(setupMasterConnector.getAvailableSetups()).thenReturn(availableModules);

        Mockito.when(inboundSetupDAO.queryAll()).thenReturn(Collections.singletonList(new InboundSetupEntity()));
        Mockito.when(persistenceV3SetupDAO.queryAll()).thenReturn(Collections.singletonList(new PersistenceV3SetupEntity()));
    }

    /**
     * Test of getKpiOverview method, of class KpiRestService.
     */
    @Test
    public void testGetKpiOverview() {
        Response response = kpiRestService.getKpiOverview(null);

        assertEquals(200, response.getStatus());
        Object bodyObject = response.getEntity();
        assert bodyObject instanceof String;
        String body = (String) bodyObject;
        JsonObject responseJson = new JsonObject(body);
        Set<String> keySet = responseJson.getMap().keySet();

        assertTrue(keySet.contains("timestamp"));
        assertTrue(keySet.contains("heartbeatInterval"));
        assertTrue(keySet.contains("availableSetups"));
        assertTrue(keySet.contains("registeredModules"));
        System.out.println(responseJson.encodePrettily());
        assertTrue(heartBeatTimeout == responseJson.getInteger("heartbeatInterval").intValue());
        List<JsonObject> returnedModules = new ArrayList();
        for (Object m : responseJson.getJsonArray("availableSetups")) {
            returnedModules.add((JsonObject) m);
        }
        returnedModules.stream()
                .map(m -> m.getString("name"))
                .forEach(name -> assertTrue(availableModules.stream().map(Enum::name).anyMatch(name::equals)));
        responseJson.getJsonArray("registeredModules").forEach(m -> assertTrue(moduleList.stream().map(UPModuleEntity::getId).anyMatch(((JsonObject) m).getString("moduleId")::equals)));
    }

    @Test
    public void testGetKpiOverview_willNotReturnInfoOnBackchannel_ifNoSetupInDatabase() {
        Response response = kpiRestService.getKpiOverview(null);

        JsonObject responseBody = new JsonObject((String) response.getEntity());
        JsonArray setups = responseBody.getJsonArray("availableSetups");
        for (Object o : setups) {
            JsonObject setup = (JsonObject) o;
            if (setup.getString("name").equals("Backchannel")) {
                assertTrue(setup.getInteger("count") == 0);
            }
        }
    }


    @Test
    public void testGetKpiOverview_willReturnInfoOnBackchannel_ifSetupInDatabase() {
        Response response = kpiRestService.getKpiOverview(null);

        JsonObject responseBody = new JsonObject((String) response.getEntity());
        JsonArray setups = responseBody.getJsonArray("availableSetups");
        for (Object o : setups) {
            JsonObject setup = (JsonObject) o;
            if (setup.getString("name").equals("Backchannel")) {
                assertTrue(setup.getInteger("count") == 1);
            }
        }
    }

}
