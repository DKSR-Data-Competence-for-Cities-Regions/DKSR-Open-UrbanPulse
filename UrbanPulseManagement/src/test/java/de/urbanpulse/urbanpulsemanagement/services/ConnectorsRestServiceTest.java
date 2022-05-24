package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.ConnectorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.TransferObjectFactory;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.ConnectorsRestFacade;
import de.urbanpulse.urbanpulsemanagement.transfer.ConnectorsWrapperTO;
import de.urbanpulse.urbanpulsemanagement.transfer.SensorsWrapperTO;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.json.JsonObject;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapperException;
import java.io.StringReader;
import java.util.Collections;
import javax.json.Json;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectorsRestServiceTest {

    private static final String CONNECTOR_ID = "42";
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/connectors/4711");
    @Mock
    ConnectorsRestFacade mockConnectorsRestFacade;
    @InjectMocks
    private ConnectorsRestService connectorsRestService;
    @Mock(name = "sensorDao")
    private SensorManagementDAO mockSensorDao;
    @Mock(name = "connectorDao")
    private ConnectorManagementDAO mockConnectorDao;
    @Mock
    private UriInfo mockContext;
    @Mock
    private UriBuilder mockUriBuilder;
    @Mock
    private ConnectorTO mockConnectorTO;

    @Mock
    private SensorTO mockSensorTO;

    @Mock
    private JsonObject mockJsonObject;

    @Mock
    private io.vertx.core.json.JsonObject mockJsonObjectVertx;

    @Mock
    private SensorModuleUpdateWrapper mockSensorModuleUpdateWrapper;

    @Mock
    private ConnectorsWrapperTO mockConnectorsWrapperTO;

    @Mock
    private TransferObjectFactory mockTransferObjectFactory;

    @Mock
    private ConnectorEntity mockConnectorEntity;

    @Mock
    private SensorEntity mockSensorEntity;

    @Mock
    private SensorsWrapperTO mockSensorsWrapperTO;

    public ConnectorsRestServiceTest() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void getAllConnectors_Success() {
        List<ConnectorTO> connectors = new ArrayList<>();
        connectors.add(mockConnectorTO);

        given(mockConnectorDao.getAll()).willReturn(connectors);
        given(mockConnectorTO.toJson()).willReturn(mockJsonObject);

        Response response = connectorsRestService.getAllConnectors();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getConnectorById_Success() {
        Response response = connectorsRestService.getAllConnectors();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetConnector_IdNotExistent_ShouldReturn404NotFound() {
        given(mockConnectorDao.getById(CONNECTOR_ID)).willReturn(null);

        Response response = connectorsRestService.getConnector(CONNECTOR_ID);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteConnector_Success() {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        given(mockSensorDao.getAll()).willReturn(mockSensors);
        given(mockSensorTO.getSenderid()).willReturn(CONNECTOR_ID + 1);
        given(mockConnectorDao.deleteById(CONNECTOR_ID)).willReturn(CONNECTOR_ID);

        Response response = connectorsRestService.deleteConnector(CONNECTOR_ID);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteConnector_updatesConnectorAuth() throws Exception {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        given(mockSensorDao.getAll()).willReturn(mockSensors);
        given(mockSensorTO.getSenderid()).willReturn(CONNECTOR_ID + 1);
        given(mockConnectorDao.deleteById(CONNECTOR_ID)).willReturn(CONNECTOR_ID);

        connectorsRestService.deleteConnector(CONNECTOR_ID);

        ArgumentCaptor<String> argument1 = ArgumentCaptor.forClass(String.class);
        verify(mockSensorModuleUpdateWrapper).unregisterConnector(argument1.capture());
        assertEquals(CONNECTOR_ID, argument1.<String>getValue());
    }

    @Test
    public void deleteConnector_Conflict() {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        given(mockSensorDao.getAll()).willReturn(mockSensors);
        given(mockSensorTO.getSenderid()).willReturn(CONNECTOR_ID);

        Response response = connectorsRestService.deleteConnector(CONNECTOR_ID);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        verify(mockSensorDao, never()).deleteById(CONNECTOR_ID);
    }

    @Test
    public void deleteConnector_DeletsFromDatabaseAndReturns202IfInboundReturnsError() throws SensorModuleUpdateWrapperException {
        List<SensorTO> mockSensors = new ArrayList<>();
        mockSensors.add(mockSensorTO);

        given(mockSensorDao.getAll()).willReturn(mockSensors);
        given(mockSensorTO.getSenderid()).willReturn(CONNECTOR_ID + 1);
        given(mockConnectorDao.deleteById(CONNECTOR_ID)).willReturn(CONNECTOR_ID);
        Mockito.doThrow(new SensorModuleUpdateWrapperException()).doNothing().when(mockSensorModuleUpdateWrapper)
                .unregisterConnector(CONNECTOR_ID);

        Response response = connectorsRestService.deleteConnector(CONNECTOR_ID);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(mockConnectorDao, times(1)).deleteById(CONNECTOR_ID);
    }

    @Test
    public void createConnector_returnsExpected() {
        String descriptionAsString = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.createConnector(anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = connectorsRestService.createConnector(descriptionAsString, mockContext, mockConnectorsRestFacade);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void createConnector_createsConnectorInDatabaseAndReturns202IfInboundReturnsError() throws SensorModuleUpdateWrapperException {
        String descriptionAsString = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.createConnector(anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");

        given(mockConnectorTO.getKey()).willReturn("Key");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);
        Mockito.doThrow(new SensorModuleUpdateWrapperException()).doNothing().when(mockSensorModuleUpdateWrapper)
                .registerConnector(anyString(), anyString());

        Response response = connectorsRestService.createConnector(descriptionAsString, mockContext, mockConnectorsRestFacade);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        //calls with value of field "description" of descriptionAsString as String
        verify(mockConnectorDao, times(1)).createConnector("{\"name\":\"desc\"}");
    }

    @Test
    public void testCreateConnector_ShouldSucceed() {
        String json = "{\"description\":{\"name\":\"TestConnector\"}}";

        given(mockConnectorDao.createConnector(anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = connectorsRestService.createConnector(json, mockContext, mockConnectorsRestFacade);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConnector_ShouldFailWithHttp400() {
        String json = "{\"description\":{\"name\":\"TestConnector\"}}";

        Response response = connectorsRestService.createConnector(json, mockContext, mockConnectorsRestFacade);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void createConnector_updatesConnectorAuth() throws Exception {
        String Json = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.createConnector(anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");
        given(mockConnectorTO.getKey()).willReturn("Key");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        connectorsRestService.createConnector(Json, mockContext, mockConnectorsRestFacade);

        ArgumentCaptor<String> argument1 = ArgumentCaptor.forClass(String.class);
        verify(mockSensorModuleUpdateWrapper).registerConnector(argument1.capture(), anyString());
        assertEquals("4711", argument1.<String>getValue());
    }

    @Test
    public void createConnector_CannotCreateConnector() {
        String Json = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.createConnector(anyString())).willReturn(null);

        Response response = connectorsRestService.createConnector(Json, mockContext, mockConnectorsRestFacade);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void createConnector_DescriptionMissing() {
        String Json = "{}";

        Response response = connectorsRestService.createConnector(Json, mockContext, mockConnectorsRestFacade);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void test_createConnector_nameMissing_returnsUnprocessableEntity() {
        String json = "{\"description\":{}}";
        Response response = connectorsRestService.createConnector(json, mockContext, mockConnectorsRestFacade);
        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void test_createConnector_duplicateName_returnsConflict() {
        String description = "{\"name\":\"non-unique-name\"}";
        String existingConnectorJson = "{\"id\":\"4711\", \"description\": " + description + "}";
        String newConnectorJson = "{\"description\": " + description + "}";

        given(mockConnectorDao.getAll()).willReturn(Collections.singletonList(mockConnectorTO));
        given(mockConnectorTO.toJson()).willReturn(Json.createReader(new StringReader(existingConnectorJson)).readObject());

        Response response = connectorsRestService.createConnector(newConnectorJson, mockContext, mockConnectorsRestFacade);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateConnector_DescriptionMissing() {
        String Json = "{}";

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, Json);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void updateConnector_returnsExpected() throws OperationNotSupportedException {
        String Json = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.updateConnector(anyString(), anyString())).willReturn(mockConnectorTO);

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, Json);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateConnector_UpdatesConnectorInDatabaseAndReturns202IfInboundReturnsError() throws OperationNotSupportedException, SensorModuleUpdateWrapperException {
        String descriptionAsString = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.updateConnector(anyString(), anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");
        given(mockConnectorTO.getKey()).willReturn("Key");

        Mockito.doThrow(new SensorModuleUpdateWrapperException()).doNothing().when(mockSensorModuleUpdateWrapper)
                .updateConnector(anyString(), anyString());

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, descriptionAsString);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        //calls with value of field "description" of descriptionAsString as String
        verify(mockConnectorDao, times(1)).updateConnector(CONNECTOR_ID, "{\"name\":\"desc\"}");
    }

    @Test
    public void testUpdateConnector_ShouldSucceed() throws OperationNotSupportedException {
        String json = "{\"description\":{\"name\":\"TestConnector\"}}";

        given(mockConnectorDao.updateConnector(anyString(), anyString())).willReturn(mockConnectorTO);

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, json);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateConnector_ShouldFailWithHttp400() throws OperationNotSupportedException {
        String json = "{\"description\":{\"name\":\"TestConnector\"}}";

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, json);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateConnector_updatesConnectorAuth() throws Exception {
        String Json = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.updateConnector(anyString(), anyString())).willReturn(mockConnectorTO);
        given(mockConnectorTO.getId()).willReturn("4711");

        given(mockConnectorTO.getKey()).willReturn("Key");

        connectorsRestService.updateConnector(CONNECTOR_ID, Json);

        ArgumentCaptor<String> argument1 = ArgumentCaptor.forClass(String.class);
        verify(mockSensorModuleUpdateWrapper).updateConnector(argument1.capture(), anyString());
        assertEquals("4711", argument1.<String>getValue());
    }

    @Test
    public void updateConnector_failed() throws OperationNotSupportedException {
        String Json = "{\"description\":{\"name\":\"desc\"}}";

        given(mockConnectorDao.updateConnector(anyString(), anyString())).willReturn(null);

        Response response = connectorsRestService.updateConnector(CONNECTOR_ID, Json);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void test_updateConnector_duplicateNameWithDifferentId_returnsConflict() {
        String description = "{\"name\":\"non-unique-name\"}";
        String existingConnectorJson = "{\"id\":\"4711\", \"description\": " + description + "}";
        String newConnectorJson = "{\"description\": " + description + "}";

        given(mockConnectorDao.getAll()).willReturn(Collections.singletonList(mockConnectorTO));
        given(mockConnectorTO.toJson()).willReturn(Json.createReader(new StringReader(existingConnectorJson)).readObject());

        Response response = connectorsRestService.updateConnector("0815", newConnectorJson);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getSensors_returnsSensorsForConnector() {
        List<SensorEntity> mockSensorEntities = new ArrayList<>();
        mockSensorEntities.add(mockSensorEntity);

        List<SensorTO> mockSensorTOs = new ArrayList<>();
        mockSensorTOs.add(mockSensorTO);

        given(mockConnectorDao.queryById(CONNECTOR_ID)).willReturn(mockConnectorEntity);
        given(mockConnectorEntity.getSensors()).willReturn(mockSensorEntities);

        given(mockSensorTO.toJson()).willReturn(mockJsonObjectVertx);
        given(mockTransferObjectFactory.createList(mockSensorEntities, SensorEntity.class, SensorTO.class)).willReturn(mockSensorTOs);
        Response response = connectorsRestService.getSensorsForConnector(CONNECTOR_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        io.vertx.core.json.JsonObject jsonObject = new io.vertx.core.json.JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("sensors"));
        io.vertx.core.json.JsonArray categories = jsonObject.getJsonArray("sensors");
        assertEquals(1, categories.size());
        assertTrue(categories.getValue(0) instanceof io.vertx.core.json.JsonObject);
    }
}
