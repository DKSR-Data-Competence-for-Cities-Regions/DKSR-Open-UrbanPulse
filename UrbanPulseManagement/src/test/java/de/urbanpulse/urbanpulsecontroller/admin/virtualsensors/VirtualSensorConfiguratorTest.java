package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import static de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.TestData.VIRTUAL_SENSOR_REQUEST;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualSensorConfiguratorTest {

    @Mock
    CategoryManagementDAO mockedCategoryManagmentDAO;

    JsonObject mockedEventType = Json.createReader(new StringReader("{\"eventtype\":\"TestEventType\",\"parameter\":\"testparameter\"}")).readObject();

    @Mock
    SensorTO mockedSensor;
    @Mock
    SensorTO mockedSensor2;
    @Mock
    SensorTO mockedSensor3;

    @Mock
    CategoryWithChildrenTO mockedRelevantCategory;
    @Mock
    CategoryWithChildrenTO mockedRelevantCategory2;
    @Mock
    CategoryWithChildrenTO mockedRelevantCategory3;

    @Mock
    CategoryTO mockedCategory;


    @InjectMocks
    VirtualSensorConfigurator virtualSensorConfigurator;

    @Test
    public void createResultStatementTest() {

        JsonObject config = Json.createObjectBuilder()
                .add("value","string")
                .add("id", "long")
                .add("SID","String")
                .add("timestamp","java.util.Date").build();


        JsonObject resultEventType = Json.createObjectBuilder()
                .add("name", "ResultEventType")
                .add("config",config)
                .add("description",Json.createObjectBuilder().build()).build();
        JsonObject result = virtualSensorConfigurator.createResultStatement(resultEventType);
        String query = result.getString("query");

        assertEquals("select '<SID_PLACEHOLDER>' as SID, current_timestamp().toDate() as timestamp, value, id from ResultEventType", query);
    }

    @Test
    public void validateResultEventTypeTest() throws VirtualSensorConfigurationException {
        JsonObject config = Json.createObjectBuilder()
                .add("value","string")
                .add("id", "long")
                .add("SID","string")
                .add("timestamp","java.util.Date").build();
        JsonObject eventtype =Json.createObjectBuilder()
                .add("config", config).build();

        virtualSensorConfigurator.validateResultEventType(eventtype);

    }

    @Test(expected = VirtualSensorConfigurationException.class)
    public void validateResultEventTypeTest_MISSING_SID_ExpectException() throws VirtualSensorConfigurationException {
        JsonObject config = Json.createObjectBuilder()
                .add("value","string")
                .add("id", "long")
                .add("timestamp","java.util.Date").build();
        JsonObject eventtype =Json.createObjectBuilder()
                .add("config", config).build();

        virtualSensorConfigurator.validateResultEventType(eventtype);
    }

    @Test(expected = VirtualSensorConfigurationException.class)
    public void validateResultEventTypeTest_WRONG_TIMESTAMP_TYPE_ExpectException() throws VirtualSensorConfigurationException {
        JsonObject config = Json.createObjectBuilder()
                .add("value","string")
                .add("id", "long")
                .add("SID","string")
                .add("timestamp","string").build();
        JsonObject eventtype =Json.createObjectBuilder()
                .add("config", config).build();

        virtualSensorConfigurator.validateResultEventType(eventtype);
    }

    @Test
    public void test_createVirtualSensorConfiguration() throws VirtualSensorConfigurationException {
        VirtualSensorConfiguration config = virtualSensorConfigurator.createVirtualSensorConfiguration(Json.createReader(new StringReader(VIRTUAL_SENSOR_REQUEST)).readObject());
        assertEquals("thePersistence", config.getTargets().get(0));
    }


}
