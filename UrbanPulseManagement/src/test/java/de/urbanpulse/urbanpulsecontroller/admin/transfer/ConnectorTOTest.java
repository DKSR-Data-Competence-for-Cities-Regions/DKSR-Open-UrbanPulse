package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import java.util.LinkedList;
import java.util.List;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectorTOTest {

    private ConnectorTO connector;

    @Mock
    private ConnectorEntity mockEntity;

    @Mock
    private SensorEntity mockSensor;

    final String EXPECTED_DESCRIPTION = "{\"name\":\"my little connector\"}";
    final String EXPECTED_ID = "00000000-0000-0000-0000-000000000013";
    final String EXPECTED_KEY = "theKeyTheSecret";
    final String SENSOR_ID = "00000000-0000-0000-0000-000000004711";
    final String EXPECTED_SENSORS = "[\"" + SENSOR_ID + "\"]";

    @Before
    public void setUp() {
        given(mockSensor.getId()).willReturn(SENSOR_ID);

        List<SensorEntity> sensors = new LinkedList<>();
        sensors.add(mockSensor);

        given(mockEntity.getId()).willReturn(EXPECTED_ID);
        given(mockEntity.getKey()).willReturn(EXPECTED_KEY);
        given(mockEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);
        given(mockEntity.getSensors()).willReturn(sensors);

        connector = new ConnectorTO(mockEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = connector.toJson();

        assertEquals("" + EXPECTED_ID, json.getString("id"));
        assertEquals(EXPECTED_KEY, json.getString("key"));
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
        assertEquals(EXPECTED_SENSORS, json.getJsonArray("sensors").toString());
    }
}
