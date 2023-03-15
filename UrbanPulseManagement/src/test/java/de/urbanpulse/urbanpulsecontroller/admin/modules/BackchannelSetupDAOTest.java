package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.ConnectorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.BackchannelSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.junit.Assert;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class BackchannelSetupDAOTest {

    protected final String expectedSetupJson = "{\"connectors\":{\"00000000-0000-0000-0000-000000000001\":{\"hmacKey\":\"53e0169a2b7f4a770cfc86af8cf5e94e\",\"backchannelKey\":\"c6180e762d8403acace22e36bb042ba2\",\"backchannelEndpoint\":\"https://example.org\",\"sidList\":[\"00000000-0000-0000-0000-000000000011\",\"00000000-0000-0000-0000-000000000012\"]}},\"foo\":\"bar\"}";

    @Mock
    ConnectorManagementDAO connectorDAO;

    @Mock
    EntityManager entityManager;

    @InjectMocks
    BackchannelSetupDAO backchannelSetupDAO;

    @Test
    public void testCreateModuleSetup_ShouldSucceed() {
        ConnectorEntity mockConnector = new ConnectorEntity();
        mockConnector.setId("00000000-0000-0000-0000-000000000001");
        mockConnector.setKey("53e0169a2b7f4a770cfc86af8cf5e94e");
        mockConnector.setBackchannelKey("c6180e762d8403acace22e36bb042ba2");
        mockConnector.setBackchannelEndpoint("https://example.org");

        SensorEntity mockSensor1 = new SensorEntity();
        mockSensor1.setId("00000000-0000-0000-0000-000000000011");

        SensorEntity mockSensor2 = new SensorEntity();
        mockSensor2.setId("00000000-0000-0000-0000-000000000012");

        mockConnector.setSensors(Arrays.asList(mockSensor1, mockSensor2));

        when(connectorDAO.queryAll()).thenReturn(Collections.singletonList(mockConnector));

        CriteriaBuilder mockCriteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery mockCriteriaQuery = mock(CriteriaQuery.class);
        Root mockRoot = mock(Root.class);
        TypedQuery mockQuery = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(mockCriteriaBuilder);
        when(mockCriteriaBuilder.createQuery()).thenReturn(mockCriteriaQuery);
        when(mockCriteriaQuery.from(any(Class.class))).thenReturn(mockRoot);
        when(entityManager.createQuery(mockCriteriaQuery)).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
        when(mockQuery.setHint(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.setLockMode(any())).thenReturn(mockQuery);
        BackchannelSetupEntity mockResult = new BackchannelSetupEntity() {
            {
                setId(1L);
                setSetupJson("{\"foo\": \"bar\"}");
            }
        };
        when(mockQuery.getResultList()).thenReturn(Collections.singletonList(mockResult));

        UPModuleEntity mockModule = mock(UPModuleEntity.class);
        when(mockModule.getId()).thenReturn("916eb397-f407-457a-8754-a78a7d47a235");

        JsonObject setupJson = backchannelSetupDAO.createModuleSetup(mockModule, new JsonObject());

        Assert.assertEquals(expectedSetupJson, setupJson.toString());
    }
}
