package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.CategoryRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapperException;
import de.urbanpulse.urbanpulsemanagement.transfer.SensorsWrapperTO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class SensorsRestServiceTest {

    private static final String CATEGORY_ID = "13";
    private static final String SENSOR_ID = "23";
    private static final String SENSOR_FILTER = "11,13";
    private static final String CONNECTOR_ID = "42";
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/sensors/4711");

    private static final String DESCRIPTION = "{\"desc\":\"this is a little description\"}";
    private static final String CONNECTOR_JSON_STRING = "\"" + CONNECTOR_ID + "\"";

    private static final String EVENT_TYPE_ID = "\"23\"";
    private static final String CATEGORY_IDS_ARRAY = "[\"1\",\"2\"]";

    private static final String EVENTTYPE_JSON_STRING = "[\n"
            + "{\n"
            + "	\"config\":\"{}\",\n"
            + "	\"name\":\"my little sensor event\",\n"
            + " 	\"description\":\"{}\"\n"
            + "},\n"
            + "{\n"
            + "	\"config\":\"{}\",\n"
            + "	\"name\":\"my other sensor event\",\n"
            + " 	\"description\":\"{}\"\n"
            + "}\n"
            + "]";

    private static final String CATEGORIES = "[\n"
            + "{\n"
            + "	\"name\":\"traffic\",\n"
            + " 	\"description\":\"this is the traffic category\"\n"
            + "},\n"
            + "{\n"
            + "	\"name\":\"environment\",\n"
            + " 	\"description\":\"this is the environment category\"\n"
            + "}\n"
            + "]";

    @InjectMocks
    private SensorsRestService service;

    @Mock
    private SensorModuleUpdateWrapper mockInboundInterfaceWrapper;

    @Mock
    private SensorManagementDAO mockSensorDao;

    @Mock
    private CategoryManagementDAO mockCategoryDao;

    @Mock
    private EventTypeManagementDAO mockEventTypeDao;

    @Mock
    private UriInfo mockContext;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    CategoryRestFacade mockCategoryRestFacade;

    @Mock
    private CategoryTO mockCategoryTO;

    @Mock
    private CategoryEntity mockCategoryEntity;

    @Mock
    private SensorTO mockSensorTO;

    @Mock
    private SensorTO mockSensorTO2;

    @Mock
    private SensorTO mockSensorTO3;

    @Mock
    private JsonObject mockJsonObject;

    @Mock
    private SensorsWrapperTO mockSensorsWrapperTO;

    @Test
    public void getSensors_returnsSensorsForCategory_ifGiven() {

        given(mockCategoryDao.queryById(CATEGORY_ID)).willReturn(mockCategoryEntity);

        given(mockSensorDao.getAllFromCategoryWithDeps(mockCategoryEntity, null)).willReturn(Collections.singletonList(mockSensorTO));

        given(mockSensorTO.toJson()).willReturn(mockJsonObject);

        Response response = service.getSensors(CATEGORY_ID, null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(mockSensorDao, never()).getAll();

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        JsonObject jsonObject = new JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("sensors"));
        JsonArray categories = jsonObject.getJsonArray("sensors");
        assertEquals(1, categories.size());
        assertTrue(categories.getValue(0) instanceof JsonObject);
    }

    @Test
    public void getSensors_withSensorFilter_returnsSensorsForCategory() {
        List<String> mockSensorIds = new ArrayList<>();
        mockSensorIds.add(SENSOR_ID);

        List<String> sensorFilter = Arrays.asList(SENSOR_FILTER.split(","));

        given(mockCategoryDao.queryById(CATEGORY_ID)).willReturn(mockCategoryEntity);

        SensorTO[] sensorTOs = {mockSensorTO, mockSensorTO3};
        given(mockSensorDao.getAllFromCategoryWithDeps(mockCategoryEntity, sensorFilter)).willReturn(Arrays.asList(sensorTOs));

        given(mockSensorTO.toJson()).willReturn(new JsonObject("{\"id\":11}"));

        given(mockSensorTO3.toJson()).willReturn(new JsonObject("{\"id\":13}"));

        Response response = service.getSensors(CATEGORY_ID, sensorFilter);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(mockSensorDao, never()).getAll();

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        JsonObject jsonObject = new JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("sensors"));
        JsonArray sensors = jsonObject.getJsonArray("sensors");
        assertEquals(2, sensors.size());
        JsonObject sensor1 = sensors.getJsonObject(0);
        JsonObject sensor2 = sensors.getJsonObject(1);
        assertEquals(new JsonObject("{\"id\":11}"), sensor1);
        assertEquals(new JsonObject("{\"id\":13}"), sensor2);
    }

    @Test
    public void getSensors_WithNoCategoryAndNoFilterGiven_returnsAllSensors() {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        given(mockSensorDao.getAllWithDepsFetched(null)).willReturn(mockSensors);
        given(mockSensorTO.toJson()).willReturn(mockJsonObject);

        Response response = service.getSensors(null, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        JsonObject jsonObject = new JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("sensors"));
        JsonArray categories = jsonObject.getJsonArray("sensors");
        assertEquals(1, categories.size());
        assertTrue(categories.getValue(0) instanceof JsonObject);
    }

    @Test
    public void getSensors_returnsSensors_WithNoCategoryGiven_emptyFilter() {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        Response response = service.getSensors(null, Collections.EMPTY_LIST);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        JsonObject jsonObject = new JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("sensors"));
        JsonArray categories = jsonObject.getJsonArray("sensors");
        assertEquals(0, categories.size());
    }

    @Test
    public void getSensorById_returnsSensor() {
        given(mockSensorDao.getById(SENSOR_ID)).willReturn(mockSensorTO);
        given(mockSensorTO.toJson()).willReturn(new JsonObject().put("id", "test"));

        Response response = service.getSensorById(SENSOR_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(mockSensorDao, never()).getAll();

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        JsonObject jsonObject = new JsonObject(jsonString);
        assertEquals("test", jsonObject.getString("id"));
    }

    @Test
    public void getSensorById_SensorNotFound() {
        given(mockSensorDao.getById(SENSOR_ID)).willReturn(null);

        Response response = service.getSensorById(SENSOR_ID);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteSensor_Success() {

        Response response = service.deleteSensor(SENSOR_ID);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(mockSensorDao, times(1)).deleteById(SENSOR_ID);
    }

    @Test
    public void deleteSensor_DeletesSensorFromDatabaseAndReturns202IfInboundReturnsError()
            throws SensorModuleUpdateWrapperException {

        Mockito.doThrow(new SensorModuleUpdateWrapperException()).doNothing().when(mockInboundInterfaceWrapper)
                .unregisterSensor(SENSOR_ID);

        Response response = service.deleteSensor(SENSOR_ID);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(mockSensorDao, times(1)).deleteById(SENSOR_ID);
    }

    @Test
    public void createSensor_UnprocEntityIfEventtypeMissing() {
        String Json = "{\"senderid\":" + CONNECTOR_JSON_STRING
                + ",\"categories\":" + CATEGORIES
                + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_UnprocEntityIfConnectorIdMissing() {
        String Json = "{\"eventtypes\":" + EVENTTYPE_JSON_STRING
                + ",\"categories\":" + CATEGORIES
                + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_UnprocEntityIfcategoriesAreMissing() {
        String Json = "{\"eventtypes\":" + EVENTTYPE_JSON_STRING
                + ",\"senderid\":" + CONNECTOR_JSON_STRING
                + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_UnprocEntityIfdescriptionIsMissing() {
        String Json = "{\"eventtypes\":" + EVENTTYPE_JSON_STRING + ",\"senderid\":"
                + CONNECTOR_JSON_STRING + ",\"categories\":" + CATEGORIES
                + ",\"location\":{}" + "}";

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_UnprocEntityIfLocationIsMissing() {
        String Json = "{\"eventtypes\":" + EVENTTYPE_JSON_STRING + ",\"senderid\":"
                + CONNECTOR_JSON_STRING + ",\"categories\":" + CATEGORIES
                + ",\"description\":" + DESCRIPTION
                + "}";

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_CreateSensorFaild() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":"
                + CONNECTOR_JSON_STRING + ",\"categories\":" + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION
                + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void createSensor_InvalidEventTypeId() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\": \"234535\" ,\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";

        given(mockEventTypeDao.queryById(any(String.class))).willReturn(null);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);
        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_InvalidEventTypeConfig() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\": \"234535\" ,\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getEventParameter()).willReturn(null);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);
        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void createSensor_configWithoutSIDIsInvalid() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\": \"234535\" ,\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"ASID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";
        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void createSensor_configWithoutTimestampIsInvalid() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\": \"234535\" ,\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"Atimestamp\":\"java.util.Date\",\"temp\":\"double\"}";
        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void createSensor_configWithvalidSID_Timestamp() throws ReferencedEntityMissingException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        given(mockSensorDao.createSensor((EventTypeEntity) any(), anyString(), (List<String>) any(), anyString(),
                anyString())).willReturn(mockSensorTO);
        given(mockSensorTO.getId()).willReturn("4711");

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getName()).willReturn("FirstEventType");
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(anyString())).willReturn(mockEventType);

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void createSensor_Success() throws SensorModuleUpdateWrapperException, ReferencedEntityMissingException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        given(mockSensorDao.createSensor((EventTypeEntity) any(), anyString(), (List<String>) any(), anyString(),
                anyString())).willReturn(mockSensorTO);
        given(mockSensorTO.getId()).willReturn("4711");

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getName()).willReturn("FirstEventType");
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(anyString())).willReturn(mockEventType);

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argument2 = ArgumentCaptor.forClass(String.class);
        verify(mockInboundInterfaceWrapper).registerSensor(anyString(), argument.capture(), argument2.capture());
        String eventType = argument.<String>getValue();
        assertEquals("FirstEventType", eventType);
        String connectorId = argument2.<String>getValue();
        assertEquals("42", connectorId);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void createSensor_CreatesSensorInDatabaseAndReturns202IfInboundReturnsError()
            throws ReferencedEntityMissingException, SensorModuleUpdateWrapperException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);

        given(mockSensorDao.createSensor(
                (EventTypeEntity) any(), anyString(), (List<String>) any(), anyString(), anyString()))
                .willReturn(mockSensorTO);
        given(mockSensorTO.getId()).willReturn("4711");
        given(mockEventType.getName()).willReturn("FirstEventType");
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(anyString())).willReturn(mockEventType);

        Mockito.doThrow(new SensorModuleUpdateWrapperException())
                .doNothing()
                .when(mockInboundInterfaceWrapper)
                .registerSensor(anyString(), any(), anyString());

        Response response = service.createSensor(Json, mockContext, mockCategoryRestFacade);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argument2 = ArgumentCaptor.forClass(String.class);
        verify(mockInboundInterfaceWrapper).registerSensor(anyString(), argument.capture(), argument2.capture());
        String eventType = argument.<String>getValue();
        assertEquals("FirstEventType", eventType);
        String connectorId = argument2.<String>getValue();
        assertEquals("42", connectorId);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateSensor_Success() throws ReferencedEntityMissingException, OperationNotSupportedException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        given(mockSensorDao.updateSensor(anyString(), (String) any(), anyString(), (List<String>) any(),
                anyString(), anyString())).willReturn(mockSensorTO);

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);
        given(mockEventType.getEventParameter()).willReturn(config);
        Response response = service.updateSensor(SENSOR_ID, Json);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateSensor_UpdatesSensorInDatabaseAndReturns202IfInboundReturnsError()
            throws ReferencedEntityMissingException, OperationNotSupportedException, SensorModuleUpdateWrapperException {
        String Json = "{\"eventtype\":" + EVENT_TYPE_ID + ",\"senderid\":" + CONNECTOR_JSON_STRING + ",\"categories\":"
                + CATEGORY_IDS_ARRAY + ",\"description\":" + DESCRIPTION + ",\"location\":{}" + "}";
        String config = "{\"SID\":\"string\",\"timestamp\":\"java.util.Date\",\"temp\":\"double\"}";

        given(mockSensorDao.updateSensor(anyString(), (String) any(), anyString(), (List<String>) any(),
                anyString(), anyString())).willReturn(mockSensorTO);

        Mockito.doThrow(new SensorModuleUpdateWrapperException()).doNothing().when(mockInboundInterfaceWrapper)
                .updateSensor(anyString(), any());

        EventTypeEntity mockEventType = mock(EventTypeEntity.class);
        given(mockEventType.getEventParameter()).willReturn(config);
        given(mockEventTypeDao.queryById(any(String.class))).willReturn(mockEventType);
        Response response = service.updateSensor(SENSOR_ID, Json);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(mockSensorDao, times(1)).updateSensor(anyString(), any(), anyString(), any(), anyString(), anyString());
    }
}
