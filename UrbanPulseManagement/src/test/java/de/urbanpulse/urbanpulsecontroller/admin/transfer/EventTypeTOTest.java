package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
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
public class EventTypeTOTest {
    private EventTypeTO transferObject;

    @Mock
    private EventTypeEntity mockEntity;

    @Mock
    private SensorEntity mockSensor;

    final String EXPECTED_DESCRIPTION = "{\"x\":\"y\"}";
    final String EXPECTED_CONFIG = "{\"a\":\"b\"}";
    final String EXPECTED_ID = "00000000-0000-0000-0000-000000000013";
    final String EXPECTED_NAME = "My little EventType";
    final String SENSOR_ID = "00000000-0000-0000-0000-000000004711";
    final String EXPECTED_SENSORS = "[\"" + SENSOR_ID + "\"]";

    @Before
    public void setUp() {
        given(mockSensor.getId()).willReturn(SENSOR_ID);

        List<SensorEntity> sensors = new LinkedList<>();
        sensors.add(mockSensor);

        given(mockEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);
        given(mockEntity.getEventParameter()).willReturn(EXPECTED_CONFIG);
        given(mockEntity.getId()).willReturn(EXPECTED_ID);
        given(mockEntity.getName()).willReturn(EXPECTED_NAME);
        given(mockEntity.getSensors()).willReturn(sensors);

        transferObject = new EventTypeTO(mockEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = transferObject.toJson();

        assertEquals("" + EXPECTED_ID, json.getString("id"));
        assertEquals(EXPECTED_NAME, json.getString("name"));
        assertEquals(EXPECTED_SENSORS, json.getJsonArray("sensors").toString());
        assertEquals(EXPECTED_CONFIG, json.getJsonObject("config").toString());
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());

        System.out.println(json);
    }

}
