package de.urbanpulse.cep;

import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.utils.ConfigGenerator;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CEPCommandHandlerSetupTest {

    private static final String KEY_NAME = "name";
    private static final String KEY_QUERY = "query";

    private CEPCommandHandler commandHandler;

    @Mock
    private MainVerticle mainVerticle;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private EPServiceProvider esper;

    String virtualSensorId = "virtually-anything";
    Map<String, Object> virtualSensor;
    Map<String, Object> virtualSensorResultEventType;
    Map<String, Object> virtualSensorResultStatement;
    List<Map<String, Object>> virtualSensorEventTypes;
    List<Map<String, Object>> virtualSensorStatements;
    List<Map<String, Object>> regularEventTypes;
    List<Map<String, Object>> regularStatements;

    JsonObject configMap;

    @Before
    public void setUp() {
        ConfigGenerator.index = 1;
        when(mainVerticle.getVertx()).thenReturn(Vertx.vertx());
        commandHandler = new CEPCommandHandler(mainVerticle, messageProducer, esper);

        Map<String, Object> someData = ConfigGenerator.getSetupConfig(0, "ResultEventType");
        virtualSensorResultEventType = ((List<Map<String, Object>>) someData.get("eventTypes")).get(0);
        virtualSensorResultStatement = ((List<Map<String, Object>>) someData.get("statements")).get(0);

        Map<String, Object> someMoreData = ConfigGenerator.getSetupConfig(0, "AnotherEventType");
        virtualSensorEventTypes = (List<Map<String, Object>>) someMoreData.get("eventTypes");
        virtualSensorStatements = (List<Map<String, Object>>) someMoreData.get("statements");

        Map<String, Object> evenMoreData = ConfigGenerator.getSetupConfig(0, "RegularEventType");
        regularEventTypes = (List<Map<String, Object>>) evenMoreData.get("eventTypes");
        regularStatements = (List<Map<String, Object>>) evenMoreData.get("statements");

        virtualSensor = new HashMap<>();
        virtualSensor.put("eventTypes", virtualSensorEventTypes);
        virtualSensor.put("resultEventType", virtualSensorResultEventType);
        virtualSensor.put("statements", virtualSensorStatements);
        virtualSensor.put("resultStatement", virtualSensorResultStatement);
        virtualSensor.put("virtualSensorId", virtualSensorId);

        JsonArray eventTypes = new JsonArray(regularEventTypes);
        JsonArray statements = new JsonArray(regularStatements);
        JsonArray virtualSensors = new JsonArray().add(virtualSensor);
        JsonArray listeners = new JsonArray();

        configMap = new JsonObject()
                .put(CEPCommandHandler.KEY_EVENT_TYPES, eventTypes)
                .put(CEPCommandHandler.KEY_STATEMENTS, statements)
                .put(CEPCommandHandler.KEY_VIRTUAL_SENSORS, virtualSensors)
                .put(CEPCommandHandler.KEY_LISTENERS, listeners);
    }

    @Test
    public void test_setup_registersAllEventTypesFirst_thenStatements() {
        EPAdministrator mockEPAdministrator = mock(EPAdministrator.class);
        ConfigurationOperations mockConfigurationOperations = mock(ConfigurationOperations.class);
        given(esper.getEPAdministrator()).willReturn(mockEPAdministrator);
        given(mockEPAdministrator.getConfiguration()).willReturn(mockConfigurationOperations);
        AtomicInteger invocationCount = new AtomicInteger(0);
        doAnswer((Answer) (InvocationOnMock invocation) -> {
            String name = (String) invocation.getArguments()[0];
            switch (name) {
                case "RegularEventType":
                    assertEquals(0, invocationCount.get());
                    break;
                case "AnotherEventType":
                    assertEquals(1, invocationCount.get());
                    break;
                case "ResultEventType":
                    assertEquals(2, invocationCount.get());
                    break;
            }
            invocationCount.incrementAndGet();
            return null;
        }).when(mockConfigurationOperations).addEventType(anyString(), any(Properties.class));

        doAnswer((Answer) (InvocationOnMock invocation) -> {
            String name = (String) invocation.getArguments()[0];
            switch (name) {
                case "RegularStatement":
                    assertEquals(3, invocationCount.get());
                    break;
                case "AnotherStatement":
                    assertEquals(4, invocationCount.get());
                    break;
                case "ResultStatement":
                    assertEquals(5, invocationCount.get());
                    break;
            }
            invocationCount.incrementAndGet();
            return null;
        }).when(mockEPAdministrator).createEPL(anyString(), anyString());

        commandHandler.setup(configMap);

        // Check that it has succeeded - as much as we can with mocks
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorResultEventType.get(KEY_NAME)));
        assertTrue(commandHandler.getEventTypes().containsKey((String) virtualSensorEventTypes.get(0).get(KEY_NAME)));
        assertNotNull(commandHandler.getVirtualSensor(virtualSensorId));

        assertEquals(6, invocationCount.get());
    }
}
