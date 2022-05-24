package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import javax.json.JsonObject;
import javax.json.JsonValue;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
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
public class VirtualSensorTOTest {

    private VirtualSensorTO virtualSensor;

    @Mock
    private StatementEntity mockResultStatement;

    @Mock
    private EventTypeEntity mockResultEventType;

    @Mock
    private CategoryEntity mockCategory;

    @Mock
    private VirtualSensorEntity mockEntity;

    @Mock
    private VirtualSensorEntity mockDeprecatedEntity;

    final String ID = "00000000-0000-0000-0000-000000000013";
    final String CATEGORY_ID = "00000000-0000-0000-0000-000000004711";
    final String RESULT_STATEMENT_ID = "00000000-0000-0000-0000-000000000033";
    final String RESULT_EVENT_TYPE_ID = "00000000-0000-0000-0000-000000004712";
    final String EXPECTED_DESCRIPTION = "{\"x\":\"y\"}";

    @Before
    public void setUp() {
        given(mockResultStatement.getId()).willReturn(RESULT_STATEMENT_ID);
        given(mockResultEventType.getId()).willReturn(RESULT_EVENT_TYPE_ID);
        given(mockCategory.getId()).willReturn(CATEGORY_ID);

        given(mockEntity.getId()).willReturn(ID);
        given(mockEntity.getCategory()).willReturn(mockCategory);
        given(mockEntity.getResultStatement()).willReturn(mockResultStatement);
        given(mockEntity.getResultEventType()).willReturn(mockResultEventType);
        given(mockEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);

        given(mockDeprecatedEntity.getId()).willReturn(ID);
        given(mockDeprecatedEntity.getCategory()).willReturn(mockCategory);
        given(mockDeprecatedEntity.getResultStatement()).willReturn(mockResultStatement);
        given(mockDeprecatedEntity.getResultEventType()).willReturn(null);
        given(mockDeprecatedEntity.getDescription()).willReturn(EXPECTED_DESCRIPTION);

        virtualSensor = new VirtualSensorTO(mockEntity);
    }

    @Test
    public void toJson_returnsExpected() {
        JsonObject json = virtualSensor.toJson();

        assertEquals(ID, json.getString("SID"));
        assertEquals("" + CATEGORY_ID, json.getString("category"));
        assertEquals("" + RESULT_STATEMENT_ID, json.getString("resultstatement"));
        assertEquals("" + RESULT_EVENT_TYPE_ID, json.getString("resultEventType"));
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
    }

    @Test
    public void toJson_DeprecatedReturnsExpected() {

        VirtualSensorTO deprecatedVirtualSensor = new VirtualSensorTO(mockDeprecatedEntity);

        JsonObject json = deprecatedVirtualSensor.toJson();

        assertEquals(ID, json.getString("SID"));
        assertEquals("" + CATEGORY_ID, json.getString("category"));
        assertEquals("" + RESULT_STATEMENT_ID, json.getString("resultstatement"));
        assertEquals(JsonValue.NULL, json.get("resultEventType"));
        assertEquals(EXPECTED_DESCRIPTION, json.getJsonObject("description").toString());
    }
}
