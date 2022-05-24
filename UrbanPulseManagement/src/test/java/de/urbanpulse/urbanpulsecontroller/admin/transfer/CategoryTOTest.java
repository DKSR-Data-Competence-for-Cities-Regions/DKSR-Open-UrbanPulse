package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
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
public class CategoryTOTest {

    private CategoryTO category;

    @Mock
    private CategoryEntity mockEntity;

    @Mock
    private CategoryEntity mockChild;

    @Mock
    private CategoryEntity mockParent;

    @Mock
    private SensorEntity mockSensor;

    private final String EXPECTED_SENSOR_ID = "00000000-0000-0000-0000-000000000033";
    private final String EXPECTED_ID = "00000000-0000-0000-0000-000000000013";
    private final String EXPECTED_PARENT_ID = "00000000-0000-0000-0000-000000000042";
    private final String EXPECTED_CHILD_ID = "00000000-0000-0000-0000-000000004711";

    private final String EXPECTED_NAME = "my little category";
    private final String EXPECTED_DESCRIPTION = "{\"x\":\"y\"}";

    private final String EXPECTED_CHILDREN = "[\"" + EXPECTED_CHILD_ID + "\"]";
    private final String EXPECTED_SENSORS = "[\"" + EXPECTED_SENSOR_ID + "\"]";

    @Before
    public void setUp() {
        given(mockSensor.getId()).willReturn(EXPECTED_SENSOR_ID);

        given(mockChild.getId()).willReturn(EXPECTED_CHILD_ID);
        given(mockParent.getId()).willReturn(EXPECTED_PARENT_ID);
        given(mockEntity.getId()).willReturn(EXPECTED_ID);

        given(mockEntity.getParentCategory()).willReturn(mockParent);

        List<SensorEntity> sensors = new LinkedList<>();
        sensors.add(mockSensor);

        List<CategoryEntity> children = new LinkedList<>();
        children.add(mockChild);

        given(mockEntity.getChildCategories()).willReturn(children);
        given(mockEntity.getName()).willReturn(EXPECTED_NAME);
        given(mockEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);
        given(mockEntity.getSensors()).willReturn(sensors);

        category = new CategoryTO(mockEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = category.toJson();

        assertEquals("" + EXPECTED_ID, json.getString("id"));
        assertEquals(EXPECTED_NAME, json.getString("name"));
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
        assertEquals("" + EXPECTED_PARENT_ID, json.getString("parentCategory"));
        assertEquals(EXPECTED_CHILDREN, json.getJsonArray("childCategories").toString());
        assertEquals(EXPECTED_SENSORS, json.getJsonArray("sensors").toString());
    }
}
