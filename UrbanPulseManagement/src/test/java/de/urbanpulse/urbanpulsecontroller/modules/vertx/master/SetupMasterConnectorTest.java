package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.ConnectionHandler;
import de.urbanpulse.transfer.TransferStructureFactory;
import de.urbanpulse.transfer.TransportLayer;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.InboundSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.PersistenceV3SetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.EventProcessorSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.OutboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.TransactionDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class SetupMasterConnectorTest {

    @Mock
    private UPModuleDAO moduleDAO;
    @Mock
    private TransactionDAO transactionDAO;
    @Mock
    private InboundSetupDAO inboundSetupDAO;
    @Mock
    private HeartbeatHandler heartbeatHandler;
    @Mock
    private PersistenceV3SetupDAO persistenceV3SetupDAO;

    @Mock
    private EventProcessorSetupDAO eventProcessorSetupDAO;
    @Mock
    private OutboundSetupDAO outboundSetupDAO;

    @Mock
    private ResetModulesFacade resetModulesFacade;
    @Mock
    private TransportLayer transport;
    @Mock(name = "connectionHandler")
    private ConnectionHandler connectionHandler;

    @InjectMocks
    private SetupMasterConnector setupMasterConnector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test_addModuleSetup_saves_the_added_DAOs() {
        Mockito.doReturn(UPModuleType.InboundInterface).when(inboundSetupDAO).getModuleType();
        Mockito.doReturn(UPModuleType.PersistenceV3).when(persistenceV3SetupDAO).getModuleType();
        Mockito.doReturn(UPModuleType.EventProcessor).when(eventProcessorSetupDAO).getModuleType();
        Mockito.doReturn(UPModuleType.OutboundInterface).when(outboundSetupDAO).getModuleType();


        setupMasterConnector.addModuleSetup(inboundSetupDAO);
        setupMasterConnector.addModuleSetup(persistenceV3SetupDAO);
        setupMasterConnector.addModuleSetup(eventProcessorSetupDAO);
        setupMasterConnector.addModuleSetup(outboundSetupDAO);


        assertEquals(6, setupMasterConnector.getAvailableSetups().size());
    }

    @Test
    public void test_register() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("moduleType", UPModuleType.InboundInterface.name());
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(moduleId);

        Mockito.doReturn(customModuleEntity).when(moduleDAO).create(UPModuleType.InboundInterface.name());

        setupMasterConnector.register(args, true, (result, cmd) -> {
            assertEquals(moduleId, result.getString("id"));
        });
    }

    @Test
    public void test_unregister_correct() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);

        Mockito.doReturn(moduleId).when(moduleDAO).deleteById(moduleId);

        setupMasterConnector.unregister(args, true, (result, cmd) -> {
            assertNull(result);
        });
    }

    @Test
    public void test_unregister_wrong() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);

        Mockito.doReturn(null).when(moduleDAO).deleteById(moduleId);

        setupMasterConnector.unregister(args, true, (result, cmd) -> {
            assertNotNull(result);
            assertTrue(result.containsKey("error"));
            assertEquals(moduleId, result.getString("id"));
        });
    }

    @Test
    public void test_reset_calls_all_needed_methods() {
        Mockito.doNothing().when(inboundSetupDAO).clearAssignments();
        Mockito.doNothing().when(persistenceV3SetupDAO).clearAssignments();

        Mockito.doNothing().when(transactionDAO).deleteAll();
        Mockito.doNothing().when(transport).publish(anyString(), any());

        setupMasterConnector.reset();

        Mockito.verify(inboundSetupDAO).clearAssignments();
        Mockito.verify(persistenceV3SetupDAO).clearAssignments();

        Mockito.verify(transactionDAO).deleteAll();
        Mockito.verify(transport).publish(anyString(), any());
    }

    @Test
    public void test_sendSetup_module_not_in_up_modules() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);

        //this will just simulate what has to happen if an unknown module comes in here
        //and because we currently dont have such a module we will use one of the usually correct ones
        Mockito.doReturn(UPModuleType.EventProcessor).when(eventProcessorSetupDAO).getModuleType();
        Mockito.doReturn(null).when(moduleDAO).queryById(moduleId);
        Mockito.doNothing().when(connectionHandler).sendResetConnection(anyString(), anyLong(), any());

        setupMasterConnector.addModuleSetup(eventProcessorSetupDAO);

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertNotNull(result);
            assertTrue(result.isEmpty());
            Mockito.verify(persistenceV3SetupDAO, Mockito.times(0)).unassign(anyString());
        });
    }

    @Test
    public void test_sendSetup_for_WellKnownNode() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(moduleId);
        customModuleEntity.setModuleType(UPModuleType.WellKnownNode.name());

        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(moduleId);
        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertTrue(result.containsKey(TransferStructureFactory.COMMAND_HEARTBEAT));
        });
    }

    @Test
    public void test_sendSetup_for_unknown_module_type() {
        String moduleId = "1";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(moduleId);
        customModuleEntity.setModuleType(UPModuleType.InboundInterface.name());

        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(moduleId);
        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertTrue(result.containsKey("error"));
        });
    }

    @Test
    public void test_cleanAnyConfigsUsedByNonExistingModules_recieves_a_null_setupDAO() {
        String moduleId = "1";
        String dummyStr = "test";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(moduleId);
        customModuleEntity.setModuleType(UPModuleType.OutboundInterface.name());
        JsonObject moduleSetupDummy = new JsonObject().put("dummy", dummyStr);

        Mockito.doReturn(UPModuleType.OutboundInterface).when(outboundSetupDAO).getModuleType();
        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(moduleId);
        Mockito.doReturn(moduleSetupDummy).when(outboundSetupDAO).createModuleSetup(any(), any());
        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());

        setupMasterConnector.addModuleSetup(outboundSetupDAO);

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertEquals(dummyStr, result.getString("dummy"));
            Mockito.verify(outboundSetupDAO).createModuleSetup(any(), any());
        });
    }

    @Test
    public void test_sendSetup_correct_no_reset() {
        String moduleId = "1";
        String dummyStr = "test";
        Map<String, Object> args = new HashMap<>();
        args.put("id", moduleId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(moduleId);
        customModuleEntity.setModuleType(UPModuleType.PersistenceV3.name());
        JsonObject moduleSetupDummy = new JsonObject().put("dummy", dummyStr);

        PersistenceV3SetupEntity persistenceV3SetupEntity = new PersistenceV3SetupEntity();
        persistenceV3SetupEntity.setId(1L);
        persistenceV3SetupEntity.setModuleId(moduleId);
        persistenceV3SetupEntity.setSetupJson("{}");
        List<PersistenceV3SetupEntity> setupEntitiesList = new ArrayList();
        setupEntitiesList.add(persistenceV3SetupEntity);

        Mockito.doReturn(UPModuleType.PersistenceV3).when(persistenceV3SetupDAO).getModuleType();
        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(moduleId);
        Mockito.doReturn(setupEntitiesList).when(persistenceV3SetupDAO).queryAll();
        Mockito.doReturn(moduleSetupDummy).when(persistenceV3SetupDAO).createModuleSetup(any(), any());
        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());

        setupMasterConnector.addModuleSetup(persistenceV3SetupDAO);

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertEquals(dummyStr, result.getString("dummy"));
            Mockito.verify(persistenceV3SetupDAO, Mockito.times(0)).unassign(anyString());
        });
    }



    @Test
    public void test_sendSetup_one_configs_in_use_by_a_non_existing_module() {
        String existingUpModulesId = "1";
        String nonExistentModuleId = "3";
        String dummyStr = "test";
        Map<String, Object> args = new HashMap<>();
        args.put("id", existingUpModulesId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(existingUpModulesId);
        customModuleEntity.setModuleType(UPModuleType.InboundInterface.name());
        JsonObject moduleSetupDummy = new JsonObject().put("dummy", dummyStr);

        InboundSetupEntity inboundV3SetupEntity = new InboundSetupEntity();
        inboundV3SetupEntity.setId(1L);
        inboundV3SetupEntity.setModuleId(nonExistentModuleId);
        inboundV3SetupEntity.setSetupJson("{}");
        List<InboundSetupEntity> setupEntitiesList = new ArrayList();
        setupEntitiesList.add(inboundV3SetupEntity);

        Mockito.doReturn(UPModuleType.InboundInterface).when(inboundSetupDAO).getModuleType();
        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(existingUpModulesId);
        Mockito.doReturn(null).when(moduleDAO).queryById(nonExistentModuleId);
        Mockito.doReturn(setupEntitiesList).when(inboundSetupDAO).queryAll();
        Mockito.doReturn(moduleSetupDummy).when(inboundSetupDAO).createModuleSetup(any(), any());
        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());
        Mockito.doNothing().when(inboundSetupDAO).unassign(nonExistentModuleId);

        setupMasterConnector.addModuleSetup(inboundSetupDAO);

        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertEquals(dummyStr, result.getString("dummy"));
            Mockito.verify(inboundSetupDAO).unassign(anyString());
        });
    }


    @Test
    public void test_cleanAnyConfigsUsedByNonExistingModules_empty_moduleId() {
        String correctModuleId = "1";
        String emptyModuleId = "";
        String dummyStr = "test";
        Map<String, Object> args = new HashMap<>();
        args.put("id", correctModuleId);
        UPModuleEntity customModuleEntity = new UPModuleEntity();
        customModuleEntity.setId(correctModuleId);
        JsonObject moduleSetupDummy = new JsonObject().put("dummy", dummyStr);


        Mockito.doReturn(customModuleEntity).when(moduleDAO).queryById(correctModuleId);

        Mockito.doNothing().when(heartbeatHandler).receivedHeartbeatForModule(anyString(), any());



        setupMasterConnector.sendSetup(args, true, (result, command) -> {
            assertFalse(result.isEmpty());
            assertEquals(dummyStr, result.getString("dummy"));
        });
    }

}
