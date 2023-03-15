package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.*;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.*;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.util.EventTypesRegistrar;
import de.urbanpulse.urbanpulsemanagement.virtualsensors.VirtualSensorsCreator;
import de.urbanpulse.urbanpulsemanagement.virtualsensors.VirtualSensorsErrorResponseFactory;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class VirtualSensorsRestServiceTest {

    @InjectMocks
    private VirtualSensorsRestService service;

    @Mock
    private VirtualSensorManagementDAO mockVirtualSensorsDao;

    @Mock
    private StatementManagementDAO mockStatementManagementDAO;

    @Mock
    private VirtualSensorsCreator mockVirtualSensorCreator;

    @Mock
    private Response mockResponse;

    @Mock
    private Response mockErrorResponse;

    @Mock
    private UriInfo mockContext;

    @Mock
    private AbstractRestFacade mockFacade;

    @Mock
    private VirtualSensorTO mockVirtualSensor;

    @Mock
    private JsonObject mockVirtualSensorJson;

    @Mock
    private EventTypesRegistrar mockEventTypesRegistrar;

    @Mock
    private VirtualSensorEntity mockVirtualSensorEntity;

    @Mock
    private StatementEntity mockResultStatement;

    @Mock
    private EventProcessorWrapper mockEventProcessor;

    @Mock
    private StatementTO mockStatementTO;

    @Mock
    private StatementTO mockStatement2TO;

    @Mock
    private StatementTO mockResultStatementTO;

    @Mock
    private OutboundInterfacesManagementDAO mockOutboundDAO;

    @Mock
    private VirtualSensorsErrorResponseFactory mockErrorResponseFactory;

    private static final String JSON_STRING = "{}";

    private static final String ID_STRING = "13";
    private static final String SID = ID_STRING;

    private static final String EVENT_TYPE_IDS = "[\"2001\"]";
    private static final String CATEGORY_ID = "4711";

    private static final String STATEMENT_ID = "1";
    private static final String STATEMENT_ID_2 = "2";
    private static final String RESULT_STATEMENT_ID = "3";
    private static final String RESULT_EVENTTYPE_ID = UUID.randomUUID().toString();

    private final static String STATEMENT_NAME = "myLittleStatement";
    private final static String STATEMENT_NAME_2 = "myOtherLittleStatement";
    private final static String RESULT_STATEMENT_NAME = "myLittleResultStatement";

    private List<VirtualSensorTO> dummySensors;
    private JsonObject expectedSensorsWrapperJson;

    @Mock
    private UpdateListenerTO mockListener;

    @Before
    public void setUp() {
        given(mockVirtualSensorCreator.createVirtualSensor(
                any(JsonObject.class), eq(mockContext), eq(mockFacade))).willReturn(mockResponse);

        given(mockVirtualSensor.toJson()).willReturn(mockVirtualSensorJson);

        dummySensors = new LinkedList<>();
        VirtualSensorTO sensor = new VirtualSensorTO();
        sensor.setId(SID);
        sensor.setDescription("{}");
        sensor.setCategoryId(CATEGORY_ID);
        sensor.setResultStatementId(RESULT_STATEMENT_ID);
        sensor.setResultEventTypeId(RESULT_EVENTTYPE_ID);
        dummySensors.add(sensor);
        expectedSensorsWrapperJson = Json.createObjectBuilder().add(
                "virtualsensors", Json.createArrayBuilder().add(sensor.toJson()).build()).build();

        given(mockVirtualSensorEntity.getResultStatement()).willReturn(mockResultStatement);
        String statementsIdsJson = Json.createArrayBuilder().add(STATEMENT_ID).add(STATEMENT_ID_2).build().toString();
        given(mockVirtualSensorEntity.getStatementIds()).willReturn(statementsIdsJson);
        given(mockVirtualSensorEntity.getEventTypeIds()).willReturn(EVENT_TYPE_IDS);
        given(mockResultStatement.getId()).willReturn(RESULT_STATEMENT_ID);
    }

    @Test
    public void createVirtualSensor_returnsResponseFromCreatorIfJsonValid() throws Exception {
        given(mockErrorResponseFactory.getErrorResponseForMissingElements(any(JsonObject.class))).willReturn(Optional.empty());
        Response response = service.createVirtualSensor(JSON_STRING, mockContext, mockFacade);

        assertSame(mockResponse, response);
    }

    @Test
    public void createVirtualSensor_returnsErrorResponseIfElementsMissing() throws Exception {
        given(mockErrorResponseFactory.getErrorResponseForMissingElements(any(JsonObject.class))).willReturn(Optional.of(mockErrorResponse));

        Response response = service.createVirtualSensor(JSON_STRING, mockContext, mockFacade);

        verifyZeroInteractions(mockVirtualSensorCreator);

        assertSame(mockErrorResponse, response);
    }

    @Test
    public void getVirtualSensor_returnsNotFoundIfMissing() throws Exception {
        given(mockVirtualSensorsDao.getById(anyString())).willReturn(null);

        Response response = service.getVirtualSensor(SID);

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void getVirtualSensor_returnsOkAndVirtualSensorIfFound() throws Exception {
        given(mockVirtualSensorsDao.getById(ID_STRING)).willReturn(mockVirtualSensor);

        Response response = service.getVirtualSensor(SID);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertSame(mockVirtualSensorJson, response.getEntity());
    }
    
    @Test
    public void getVirtualSensors_all_conditions() {
               
        when(mockVirtualSensorsDao.getFilteredByCategory(anyString())).thenReturn(new ArrayList<>());
        when(mockVirtualSensorsDao.getFilteredBySchema(anyString())).thenReturn(new ArrayList<>());
        when(mockVirtualSensorsDao.getFilteredByResultStatementName(anyString())).thenReturn(new ArrayList<>());
        when(mockVirtualSensorsDao.getAll()).thenReturn(new ArrayList<>());
        
        service.getVirtualSensors(null, null, null);
        verify(mockVirtualSensorsDao,times(1)).getAll();
        //service.getVirtualSensors(CATEGORY_ID, RESULT_STATEMENT_NAME, STATEMENT_NAME)
        service.getVirtualSensors("id", null, null);
        verify(mockVirtualSensorsDao,times(1)).getFilteredByCategory(anyString());
        
        service.getVirtualSensors(null, "id", null);
        verify(mockVirtualSensorsDao,times(1)).getFilteredByResultStatementName(anyString());
        
        service.getVirtualSensors(null, null, "id");
        verify(mockVirtualSensorsDao,times(1)).getFilteredBySchema(anyString());
        
        
        //verify(mockVirtualSensorsDao,times(1)).getFilteredByCategory(anyString());
    }

    @Test
    public void getVirtualSensors_returnsAllForNullCategoryId() throws Exception {
        given(mockVirtualSensorsDao.getAll()).willReturn(dummySensors);

        Response response = service.getVirtualSensors(null, null, null);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonObject wrapperJson = (JsonObject) response.getEntity();
        assertEquals(expectedSensorsWrapperJson, wrapperJson);
    }

    @Test
    public void getVirtualSensors_returnsFromCategoryForCategoryId() throws Exception {

        given(mockVirtualSensorsDao.getFilteredByCategory(CATEGORY_ID)).willReturn(dummySensors);

        Response response = service.getVirtualSensors(CATEGORY_ID, null, null);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonObject wrapperJson = (JsonObject) response.getEntity();
        assertEquals(expectedSensorsWrapperJson, wrapperJson);
    }

    @Test
    public void getVirtualSensors_returns400IfBothCategoryAndResultStatementIdGiven() throws Exception {
        Response response = service.getVirtualSensors(CATEGORY_ID, RESULT_STATEMENT_NAME, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void getVirtualSensors_returnsWithResultStatementNameForResultStatementName() throws Exception {
        given(mockVirtualSensorsDao.getFilteredByResultStatementName(RESULT_STATEMENT_NAME)).willReturn(dummySensors);

        Response response = service.getVirtualSensors(null, RESULT_STATEMENT_NAME, null);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonObject wrapperJson = (JsonObject) response.getEntity();
        assertEquals(expectedSensorsWrapperJson, wrapperJson);
    }

    @Test
    public void deleteVirtualSensor_returnsNoContentIfNotFound() throws Exception {
        given(mockVirtualSensorsDao.queryById(anyString())).willReturn(null);

        Response response = service.deleteVirtualSensor(SID);

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteVirtualSensor_deletesSensorThenDependentStatementsThenEventTypesThenNotificesCEP() throws Exception {
        given(mockVirtualSensorsDao.queryById(ID_STRING)).willReturn(mockVirtualSensorEntity);

        Response response = service.deleteVirtualSensor(SID);

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        InOrder inOrder = inOrder(mockVirtualSensorsDao, mockStatementManagementDAO, mockEventProcessor, mockEventTypesRegistrar);
        inOrder.verify(mockVirtualSensorsDao).deleteById(ID_STRING);

        inOrder.verify(mockEventProcessor, never()).unregisterStatement(STATEMENT_NAME_2);
        inOrder.verify(mockStatementManagementDAO).deleteById(STATEMENT_ID_2);

        inOrder.verify(mockEventProcessor, never()).unregisterStatement(STATEMENT_NAME);
        inOrder.verify(mockStatementManagementDAO).deleteById(STATEMENT_ID);

        inOrder.verify(mockEventProcessor, never()).unregisterStatement(RESULT_STATEMENT_NAME);
        inOrder.verify(mockStatementManagementDAO).deleteById(RESULT_STATEMENT_ID);

        inOrder.verify(mockEventTypesRegistrar).deleteEventTypeById("2001", false);

        inOrder.verify(mockEventProcessor).unregisterVirtualSensor(SID);
    }

    @Test
    public void deleteVirtualSensor_returnsConflictIfListenersStillRegistered() throws Exception {
        List<UpdateListenerTO> listeners = new LinkedList<>();
        listeners.add(mockListener);
        given(mockOutboundDAO.getUpdateListenersOfStatement(anyString())).willReturn(listeners);

        given(mockVirtualSensorsDao.queryById(ID_STRING)).willReturn(mockVirtualSensorEntity);

        Response response = service.deleteVirtualSensor(SID);

        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void test_deleteVirtualSensor_doesNotUnregisterEventTypesOrStatementsDirectlyFromCEP() throws EventProcessorWrapperException {
        given(mockVirtualSensorsDao.queryById(ID_STRING)).willReturn(mockVirtualSensorEntity);

        Response response = service.deleteVirtualSensor(SID);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(mockEventProcessor, never()).unregisterEventType(anyString());
        verify(mockEventProcessor, never()).unregisterStatement(anyString());
    }
}
