package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.ModuleUpdateManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;



/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class EventProcessorWrapperTest {

    @InjectMocks
    private EventProcessorWrapper wrapper;

    @Mock
    private CommandsJsonFactory mockCommandsJsonFactory;

    @Mock
    private ModuleUpdateManager mockModuleUpdateManager;

    @Mock
    private OutboundInterfacesManagementDAO mockOoutboundInterfacesManagementDAO;

    @Mock
    private JsonObject mockCommand;

    @Test
    public void registerStatement_doesNotThrowOnValidStatementSyntax_bug3046() throws Exception {
        given(mockModuleUpdateManager.runModuleTypeCommands(any())).willReturn(Collections.emptyList());
        given(mockCommandsJsonFactory.createRegisterStatementCommands(anyString(), anyString())).willReturn(new JsonObject());
        wrapper.registerStatement("validSyntax", "goodStatement");

        verify(mockCommandsJsonFactory).createRegisterStatementCommands("goodStatement", "validSyntax");
        verify(mockModuleUpdateManager).runModuleTypeCommands(any(JsonObject.class));
    }

    @Test
    public void testCountProcessedEvents() throws Exception {
        JsonObject result = new JsonObject(
                "{\"header\":{\"senderId\":\"296edcc4-fdec-425d-b6bb-1a46334f31c4\",\"messageSN\":2,\"inReplyTo\":2},\"body\":{\"processedEvents\":110193632}}");
        given(mockCommandsJsonFactory.createCountProcessedEventsCommand()).willReturn(mockCommand);
        given(mockModuleUpdateManager.runSingleInstanceReturnCommand(eq(mockCommand), any(CountProcessedEventsResultVerifier.class)))
                .willReturn(result);
        assertEquals(110193632, (long) wrapper.countProcessedEvents());
    }

    @Test
    public void test_registerVirtualSensor_succeeds_ifRunModuleTypeCommandsSucceeds() throws Exception {
        given(mockModuleUpdateManager.runModuleTypeCommands(any())).willReturn(Collections.emptyList());
        given(mockCommandsJsonFactory.createRegisterVirtualSensorCommands(anyString(), any(),any(),any(),any())).willReturn(new JsonObject());
        wrapper.registerVirtualSensor("virtualSensorId",
                new JsonArray(),
                new JsonArray(),
                new JsonObject(),
                new JsonObject());
        verify(mockCommandsJsonFactory).createRegisterVirtualSensorCommands(eq("virtualSensorId"), any(), any(), any(), any());
        verify(mockModuleUpdateManager).runModuleTypeCommands(any(JsonObject.class));
    }

    @Test(expected = EventProcessorWrapperException.class)
    public void test_registerVirtualSensor_throwsEventProcessorWrapperException_ifRunModuleTypeCommandsFails() throws Exception {
        given(mockModuleUpdateManager.runModuleTypeCommands(any())).willReturn(Collections.singletonList(new JsonObject()));
        wrapper.registerVirtualSensor("virtualSensorId",
                new JsonArray(),
                new JsonArray(),
                new JsonObject(),
                new JsonObject());
    }

    @Test
    public void test_unregisterVirtualSensor_succeeds_ifRunModuleTypeCommandsSucceeds() throws Exception {
        given(mockModuleUpdateManager.runModuleTypeCommands(any())).willReturn(Collections.emptyList());
        given(mockCommandsJsonFactory.createUnregisterVirtualSensorCommands(anyString())).willReturn(new JsonObject());
        wrapper.unregisterVirtualSensor("virtualSensorId");
        verify(mockCommandsJsonFactory).createUnregisterVirtualSensorCommands("virtualSensorId");
        verify(mockModuleUpdateManager).runModuleTypeCommands(any(JsonObject.class));
    }

    @Test(expected = EventProcessorWrapperException.class)
    public void test_unregisterVirtualSensor_throwsEventProcessorWrapperException_ifRunModuleTypeCommandsFails() throws Exception {
        given(mockModuleUpdateManager.runModuleTypeCommands(any())).willReturn(Collections.singletonList(new JsonObject()));
        wrapper.unregisterVirtualSensor("virtualSensorId");
    }
}
