package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.naming.OperationNotSupportedException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import static org.mockito.BDDMockito.*;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class SensorManagementDAOTest {
    
    private static final String ID = "13";
    private static final String CONNECTOR_ID_STRING = "4711";
    
    private static final String DESCRIPTION = "my little description";
    private static final String LOCATION_JSON = "{}";
    
    private static final String CATEGORY_ID = "13";
    
    @InjectMocks
    private SensorManagementDAO dao;
    
    @Mock
    private CategoryManagementDAO mockCategoryDao;
    
    @Mock
    protected EntityManager entityManager;
    
    @Mock
    private ConnectorManagementDAO mockConnectorDao;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SensorEntity sensorEntityMock;
    
    @Mock
    private SensorTO sensorToMock;
    
    @Mock
    private ConnectorEntity mockConnectorEntity;
    
    @Mock
    private EventTypeManagementDAO eventTypeDao;
    
    @Mock
    private CategoryEntity mockCategory;
    
    @Mock
    private EventTypeEntity mockEventType;
    
    @Mock
    private ConnectorEntity mockConnector;
    
    @Mock
    private CategoryEntity mockOldCategory;
    
    @Mock
    private TypedQuery mockedQuery;
    
    @Before
    public void setUp() throws Exception {
    }
    
    @Test
    public void deleteSensor_removesSensorFromCategoriesAndConnectorAndEventTypesThenDeletesAndFlushes() throws Exception {
        List<CategoryEntity> dummyCategories = new LinkedList<>();
        dummyCategories.add(mockCategory);
        
        EventTypeEntity dummyEventType = mockEventType;
        
        given(entityManager.find(SensorEntity.class, ID)).willReturn(sensorEntityMock);
        given(sensorEntityMock.getCategories()).willReturn(dummyCategories);
        given(sensorEntityMock.getEventType()).willReturn(dummyEventType);
        given(sensorEntityMock.getConnector()).willReturn(mockConnector);
        
        dao.deleteById(ID);
        
        InOrder inOrder = inOrder(mockCategory, sensorEntityMock, entityManager, mockConnector, mockEventType);
        
        inOrder.verify(mockCategory).removeSensor(sensorEntityMock);
        inOrder.verify(entityManager).merge(mockCategory);
        
        inOrder.verify(mockConnector).removeSensor(sensorEntityMock);
        inOrder.verify(entityManager).merge(mockConnector);
        
        inOrder.verify(mockEventType).removeSensor(sensorEntityMock);
        inOrder.verify(entityManager).merge(mockEventType);
        
        inOrder.verify(entityManager).remove(sensorEntityMock);
        inOrder.verify(entityManager).flush();
    }
    
    @Test
    public void createSensor_returnsSensorTO() throws Exception {
        given(mockConnectorDao.queryById(CONNECTOR_ID_STRING)).willReturn(mockConnectorEntity);
        final EventTypeEntity eventType = mockEventType;
        final LinkedList<String> categoryIds = new LinkedList<>();
        
        SensorTO sensor = dao.createSensor(eventType, CONNECTOR_ID_STRING, categoryIds, DESCRIPTION, LOCATION_JSON);
        
        assertNotNull(sensor);
        assertEquals(DESCRIPTION, sensor.getDescription());
        assertEquals(LOCATION_JSON, sensor.getLocation());
    }
    
    @Test
    public void updateSensor_returnsExpectedAndReplacesCategories() throws OperationNotSupportedException, ReferencedEntityMissingException {
        SensorEntity testSensor = new SensorEntity();
        testSensor.setId(ID);
        
        given(entityManager.find(SensorEntity.class, ID)).willReturn(testSensor);
        given(entityManager.merge(any(SensorEntity.class))).willReturn(testSensor);
        
        given(mockConnectorDao.queryById(CONNECTOR_ID_STRING)).willReturn(mockConnectorEntity);
        
        final String eventTypeId = mockEventType.getId();
        final LinkedList<String> categoryIds = new LinkedList<>();
        categoryIds.add(CATEGORY_ID);
        
        given(mockCategoryDao.queryById(CATEGORY_ID)).willReturn(mockCategory);
        
        List<CategoryEntity> oldCategories = new LinkedList<>();
        oldCategories.add(mockOldCategory);
        
        testSensor.setCategories(oldCategories);
        
        given(eventTypeDao.queryById(eventTypeId)).willReturn(mockEventType);
        
        SensorTO sensor = dao.updateSensor(ID, eventTypeId, CONNECTOR_ID_STRING, categoryIds, DESCRIPTION, LOCATION_JSON);
        
        verify(mockOldCategory).removeSensor(testSensor);
        verify(entityManager).merge(mockCategory);
        
        verify(mockCategory).addSensor(testSensor);
        verify(entityManager).merge(mockCategory);
        
        verify(entityManager).merge(testSensor);
        verify(entityManager).flush();
        
        assertNotNull(sensor);
        assertEquals(ID, sensor.getId());
    }
    
    @Test(expected = OperationNotSupportedException.class)
    public void updateSensor_throwsOperationNotSupportedException() throws OperationNotSupportedException, ReferencedEntityMissingException {
        given(entityManager.find(SensorEntity.class, ID)).willReturn(null);
        
        final String eventTypeId = mockEventType.getId();
        final LinkedList<String> categoryIds = new LinkedList<>();
        
        SensorTO sensor = dao.updateSensor(ID, eventTypeId, CONNECTOR_ID_STRING, categoryIds, DESCRIPTION, LOCATION_JSON);
        
    }
    
    @Test
    public void test_getSensorsByEventType() {
        EventTypeEntity et = new EventTypeEntity();
        et.setId("1");
        ConnectorEntity co = new ConnectorEntity();
        co.setId("1");
        SensorEntity testSensor = new SensorEntity();
        testSensor.setId(ID);
        testSensor.setCategories(new ArrayList<>());
        testSensor.setEventType(et);
        testSensor.setConnector(co);
        
        List<SensorEntity> sensors = new ArrayList<>();
        sensors.add(testSensor);
        
        given(entityManager.createQuery(anyString(), any())).willReturn(mockedQuery);
        given(mockedQuery.setParameter(eq("id"), eq("test"))).willReturn(mockedQuery);
        given(mockedQuery.getResultList()).willReturn(sensors);
        
        List<SensorTO> sensorsResult = dao.getSensorsByEventType("test");
        assertEquals(ID, sensorsResult.get(0).getId());
    }
    
}
