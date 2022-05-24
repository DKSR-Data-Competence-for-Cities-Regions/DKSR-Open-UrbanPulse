package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
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
public class SensorTOTest {

    private SensorTO sensor;

    @Mock
    private EventTypeEntity mockEventType;

    @Mock
    private CategoryEntity mockCategory;

    @Mock
    private SensorEntity mockEntity;

    @Mock
    private ConnectorEntity mockConnector;

    final String EXPECTED_ID = "00000000-0000-0000-0000-000000000013";

    final String EVENTTYPE_ID = "00000000-0000-0000-0000-000000004711";
    final String EXPECTED_EVENTTYPE = EVENTTYPE_ID.toString();

    final String CATEGORY_ID = "00000000-0000-0000-0000-000000000042";
    final String EXPECTED_CATEGORIES = "[\"" + CATEGORY_ID + "\"]";

    final String EXPECTED_SENDER_ID = "00000000-0000-0000-0000-000000000033";

    final String EXPECTED_DESCRIPTION = "{\"x\":\"y\"}";
    final String EXPECTED_LOCATION = "{\"type\":\"provided\"}";

    @Before
    public void setUp() {
        given(mockEventType.getId()).willReturn(EVENTTYPE_ID);
        given(mockCategory.getId()).willReturn(CATEGORY_ID);
        given(mockConnector.getId()).willReturn(EXPECTED_SENDER_ID);

        List<CategoryEntity> categories = new LinkedList<>();
        categories.add(mockCategory);

        EventTypeEntity eventType = mockEventType;

        given(mockEntity.getConnector()).willReturn(mockConnector);
        given(mockEntity.getCategories()).willReturn(categories);
        given(mockEntity.getEventType()).willReturn(eventType);
        given(mockEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);
        given(mockEntity.getId()).willReturn(EXPECTED_ID);
        given(mockEntity.getLocation()).willReturn(EXPECTED_LOCATION);

        sensor = new SensorTO(mockEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = sensor.toJson();

        assertEquals("" + EXPECTED_ID, json.getString("id"));
        assertEquals(EXPECTED_EVENTTYPE, json.getString("eventtype"));
        assertEquals(EXPECTED_CATEGORIES, json.getJsonArray("categories").toString());
        assertEquals(EXPECTED_LOCATION, json.getJsonObject("location").toString());
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
        assertEquals("" + EXPECTED_SENDER_ID, json.getString("senderid"));
    }

}
