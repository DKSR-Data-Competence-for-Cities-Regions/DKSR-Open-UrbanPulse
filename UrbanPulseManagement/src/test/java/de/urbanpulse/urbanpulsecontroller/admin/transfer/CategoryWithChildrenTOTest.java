package de.urbanpulse.urbanpulsecontroller.admin.transfer;


import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryWithChildrenTOTest {

    private CategoryWithChildrenTO category;

    private CategoryEntity dummyEntity;
    private CategoryEntity dummyChild;
    private CategoryEntity dummyParent;
    private EventTypeEntity dummyEventType;
    private SensorEntity dummySensor;
    private ConnectorEntity dummyConnector;

    private final String EXPECTED_ID = "00000000-0000-0000-0000-000000000013";

    private final String EXPECTED_SENSOR_ID = "00000000-0000-0000-0000-000000000033";
    private final String EXPECTED_EVENT_TYPE_ID = "00000000-0000-0000-0000-000000002001";
    private final String EXPECTED_SENSOR_DESCRIPTION = "{\"bla\":\"blub\"}";
    private final String EXPECTED_SENSOR_LOCATION = "{\"type\":\"provided\"}";

    private final String EXPECTED_SENSOR_CATEGORIES = "[\"" + EXPECTED_ID + "\"]";
    private final String EXPECTED_SENSOR_EVENTTYPE =  EXPECTED_EVENT_TYPE_ID.toString();

    private final String EXPECTED_PARENT_ID = "00000000-0000-0000-0000-000000000042";
    private final String EXPECTED_CHILD_ID = "00000000-0000-0000-0000-000000004711";

    private final String EXPECTED_NAME = "my little category";
    private final String EXPECTED_DESCRIPTION = "{\"x\":\"y\"}";

    private final String EXPECTED_CHILD_NAME = "my little big category";
    private final String EXPECTED_CHILD_DESCRIPTION = "{\"a\":\"b\"}";

    private final String EXPECTED_CONNECTOR_ID = "00000000-0000-0000-0000-000000000512";

    @Before
    public void setUp() {
        dummyEntity = new CategoryEntity();
        dummyChild = new CategoryEntity();
        dummyParent = new CategoryEntity();
        dummySensor = new SensorEntity();
        dummyEventType = new EventTypeEntity();
        dummyConnector = new ConnectorEntity();

        dummySensor.setId(EXPECTED_SENSOR_ID);
        dummySensor.setLocation(EXPECTED_SENSOR_LOCATION);

        List<CategoryEntity> sensorCategories = new LinkedList<>();
        sensorCategories.add(dummyEntity);
        dummySensor.setCategories(sensorCategories);
        dummySensor.setDescription(EXPECTED_SENSOR_DESCRIPTION);
        dummySensor.setConnector(dummyConnector);
        dummyConnector.setId(EXPECTED_CONNECTOR_ID);

        EventTypeEntity sensorEventType = dummyEventType;

        dummyEventType.setId(EXPECTED_EVENT_TYPE_ID);

        dummySensor.setEventType(sensorEventType);

        dummyChild.setSensors(new LinkedList<SensorEntity>());
        dummyChild.setChildCategories(new LinkedList<CategoryEntity>());
        dummyChild.setName(EXPECTED_CHILD_NAME);
        dummyChild.setDescription(EXPECTED_CHILD_DESCRIPTION);
        dummyChild.setParentCategory(dummyEntity);
        dummyChild.setId(EXPECTED_CHILD_ID);

        dummyParent.setId(EXPECTED_PARENT_ID);
        dummyEntity.setParentCategory(dummyParent);

        List<SensorEntity> sensors = new LinkedList<>();
        sensors.add(dummySensor);

        List<CategoryEntity> children = new LinkedList<>();
        children.add(dummyChild);

        dummyEntity.setId(EXPECTED_ID);
        dummyEntity.setChildCategories(children);
        dummyEntity.setSensors(sensors);
        dummyEntity.setName(EXPECTED_NAME);
        dummyEntity.setDescription(EXPECTED_DESCRIPTION);

        category = new CategoryWithChildrenTO(dummyEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = category.toJson();

        assertEquals("" + EXPECTED_ID, json.getString("id"));
        assertEquals(EXPECTED_NAME, json.getString("name"));
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
        assertEquals("" + EXPECTED_PARENT_ID, json.getString("parentCategory"));

        JsonArray children = json.getJsonArray("childCategories");
        assertNotNull(children);
        assertEquals(1, children.size());
        JsonObject child = children.getJsonObject(0);

        assertEquals("" + EXPECTED_CHILD_ID, child.getString("id"));
        assertEquals(EXPECTED_CHILD_DESCRIPTION, child.getJsonObject("description").toString());
        assertEquals(EXPECTED_CHILD_NAME, child.getString("name"));
        assertEquals("" + EXPECTED_ID, child.getString("parentCategory"));
        assertTrue(child.getJsonArray("childCategories").isEmpty());

        JsonArray sensors = json.getJsonArray("sensors");
        assertNotNull(sensors);
        assertEquals(1, sensors.size());
        JsonObject sensor = sensors.getJsonObject(0);

        assertEquals("" + EXPECTED_SENSOR_ID, sensor.getString("id"));
        assertEquals(EXPECTED_SENSOR_LOCATION, sensor.getJsonObject("location").toString());
        assertEquals(EXPECTED_SENSOR_EVENTTYPE, sensor.getString("eventtype"));
        assertEquals(EXPECTED_SENSOR_CATEGORIES, sensor.getJsonArray("categories").toString());
        assertEquals(EXPECTED_SENSOR_DESCRIPTION, sensor.getJsonObject("description").toString());

        System.out.println(json.toString());
    }
}
