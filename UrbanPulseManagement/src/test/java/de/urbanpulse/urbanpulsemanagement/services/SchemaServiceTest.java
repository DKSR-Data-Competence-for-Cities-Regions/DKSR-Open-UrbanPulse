package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class SchemaServiceTest {

    private static final String ID_STRING = "13";
    private static final String NAME = "myLittleEventType";
    private static final String CONFIG = "{\"timestamp\":\"java.util.Date\"}";
    private static final String DESCRIPTION = "{\"timestamp\":\"a timestamp\"}";

    @Mock
    EventTypeManagementDAO mockedDao;

    
    @Mock
    private UriInfo mockContext;

    @Mock
    private UriBuilder mockUriBuilder;
   
    
    @InjectMocks
    private SchemaService service;
    
    @Before
    public void initMock() {
        List<SensorEntity> sensors = new LinkedList<>();
        SensorEntity e = new SensorEntity();
        e.setId("4711");
        sensors.add(e);
        
        EventTypeEntity dummyEventType = new EventTypeEntity();
        dummyEventType.setId(ID_STRING);
        dummyEventType.setEventParameter(CONFIG);
        dummyEventType.setName(NAME);
        dummyEventType.setDescription(DESCRIPTION);
        dummyEventType.setSensors(sensors);

        List<EventTypeEntity> eventTypes = new ArrayList<>();
        eventTypes.add(dummyEventType);

        given(mockedDao.queryAll()).willReturn(eventTypes);
        given(mockedDao.queryById(anyString())).willReturn(dummyEventType);
        given(mockedDao.queryFilteredBy(anyString(), anyString())).willReturn(eventTypes);
        
        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(URI.create("https://local.urbanpulse.de/UrbanPulseManagement/api/sensors?schema="+dummyEventType.getName()));
        
        service.init();
    }

    /**
     * Test of getEventTypeAsJaonSchema method, of class SchemaService.
     */
    @Test
    public void testGetEventTypeAsJsonSchema() throws Exception {
        Response resp = service.getEventTypeAsJsonSchema("4711", mockContext);
       
        JsonObject schemas = new JsonObject((String)resp.getEntity());
        assertEquals("myLittleEventType", schemas.getString("title"));
        assertEquals("string", schemas.getJsonObject("properties").getJsonObject("timestamp").getString("type"));
        assertEquals("date-time", schemas.getJsonObject("properties").getJsonObject("timestamp").getString("format"));

    }

    /**
     * Test of getAllEventTypesAsJsonSchema method, of class SchemaService.
     */
    @Test
    public void testGetAllEventTypesAsJsonSchema() throws Exception {
        Response resp = service.getAllEventTypesAsJsonSchema(mockContext);
       
        JsonArray schemas = new JsonObject((String)resp.getEntity()).getJsonArray("schemas");
        
        assertEquals("myLittleEventType", schemas.getJsonObject(0).getString("title"));
        assertEquals("string", schemas.getJsonObject(0).getJsonObject("properties").getJsonObject("timestamp").getString("type"));
        assertEquals("date-time", schemas.getJsonObject(0).getJsonObject("properties").getJsonObject("timestamp").getString("format"));
    }

    /**
     * Test of getEventTypeAsJasonSchemaByName method, of class SchemaService.
     */
    @Test
    public void testGetEventTypeAsJsonSchemaByName() throws Exception {
        Response resp = service.getEventTypeAsJsonSchemaByName("myLittleEventType", mockContext);
       
        JsonArray schemas = new JsonObject((String)resp.getEntity()).getJsonArray("schemas");
        JsonObject schema = schemas.getJsonObject(0);
        assertEquals("myLittleEventType", schema.getString("title"));
        assertEquals("string", schema.getJsonObject("properties").getJsonObject("timestamp").getString("type"));
        assertEquals("date-time", schema.getJsonObject("properties").getJsonObject("timestamp").getString("format"));
    }

}
