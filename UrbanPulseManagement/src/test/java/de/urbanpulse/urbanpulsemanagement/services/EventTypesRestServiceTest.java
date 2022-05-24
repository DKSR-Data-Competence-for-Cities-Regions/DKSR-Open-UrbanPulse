package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.EventTypesRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventConfigValidator;
import de.urbanpulse.urbanpulsemanagement.util.EventTypesRegistrar;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class EventTypesRestServiceTest {

    private static final String AUTH_HEADER = "someValidAuthHeader";

    private static final String ID_STRING = "13";
    private static final String NAME = "myLittleEventType";
    private static final String CONFIG = "{\"foo\":\"bar\"}";
    private static final String DESCRIPTION = "{\"bla\":\"blub\"}";
    private static final String VALID_JSON_STRING = "{"
            + "\"config\":" + CONFIG + ","
            + "\"description\":" + DESCRIPTION + ","
            + "\"name\":\"" + NAME + "\""
            + "}";
    private static final String INVALID_JSON_STRING = "{"
            + "\"config\":" + CONFIG + ","
            + "\"name\":\"" + NAME + "\""
            + "}";

    private static final String RELATIVE_PATH_FOR_ALL_EVENT_TYPES = "/eventtypes";
    private static final String RELATIVE_PATH_FOR_EVENT_TYPE_ID = "/eventtypes/" + NAME;

    private static final URI ABSOLUTE_PATH = URI.create("https://foo.bar/eventtypes/");
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/eventtypes/" + NAME);

    @InjectMocks
    private EventTypesRestService service;

    @Mock
    private EventConfigValidator mockEventConfigValidator;

    @Mock
    private EventTypesRegistrar mockRegistrar;

    @Mock
    private UriInfo mockContext;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    private EventTypeManagementDAO dao;

    @Mock
    private EventTypeTO mockEventType;

    @Mock
    private JsonObject mockEventJson;

    private static final String EXPETCED_WRAPPER_JSON
            = "{\"eventtypes\":[{\"id\":\"13\",\"name\":\"myLittleEventType\",\"description\":{\"bla\":\"blub\"},\"config\":{\"foo\":\"bar\"},\"sensors\":[\"4711\"]}]}";

    private static final JsonObject EXPECTED_WRAPPER = Json.createReader(new StringReader(EXPETCED_WRAPPER_JSON)).readObject();

    @Before
    public void setUp() throws Exception {


        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);
        given(mockEventType.toJson()).willReturn(mockEventJson);
    }

    @Test
    public void getEventType_returnsEventType() {
        given(mockRegistrar.getEventTypeById(ID_STRING)).willReturn(mockEventType);
        Response response = service.getEventType(ID_STRING);

        assertSame(mockEventJson, response.getEntity());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void getAllEventTypes_returnsAllEventTypes() {
        List<String> sensors = new LinkedList<>();
        sensors.add("4711");

        EventTypeTO dummyEventType = new EventTypeTO();
        dummyEventType.setId(ID_STRING);
        dummyEventType.setConfig(CONFIG);
        dummyEventType.setName(NAME);
        dummyEventType.setDescription(DESCRIPTION);
        dummyEventType.setSensors(sensors);

        List<EventTypeTO> eventTypes = new ArrayList<>();
        eventTypes.add(dummyEventType);

        given(mockRegistrar.getAllEventTypes()).willReturn(eventTypes);

        Response response = service.getAllEventTypes();

        JsonObject wrapperJsonObject = (JsonObject) response.getEntity();
        assertEquals(EXPECTED_WRAPPER, wrapperJsonObject);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void createEventType_returnsCreatedWithExpectedLocationUri() {
        given(mockRegistrar.registerEventType(any(JsonObject.class))).willReturn(mockEventType);
        given(mockEventType.getId()).willReturn("13");

        Response response = service.createEventType(VALID_JSON_STRING, mockContext, EventTypesRestFacade.class);
        assertEquals(EXPECTED_LOCATION, response.getLocation());
        assertEquals(201, response.getStatus());
    }

    @Test
    public void createEventType_returnsUnprocEntityIfInvalidConfig() {
        given(mockEventConfigValidator.isInvalid(any(JsonObject.class))).willReturn(true);
        Response response = service.createEventType(VALID_JSON_STRING, mockContext, EventTypesRestFacade.class);
        assertEquals(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
        verify(mockRegistrar, never()).registerEventType(any(JsonObject.class));
    }

    @Test
    public void createEventType_returnsUnprocEntityIfInvalidJson() {
        Response response = service.createEventType(INVALID_JSON_STRING, mockContext, EventTypesRestFacade.class);
        assertEquals(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
        verify(mockRegistrar, never()).registerEventType(any(JsonObject.class));
    }

    @Test
    public void deleteEventType_returnsNoContent() {
        given(mockRegistrar.deleteEventTypeById(ID_STRING)).willReturn(ID_STRING);

        Response response = service.deleteEventType(ID_STRING);

        verify(mockRegistrar).deleteEventTypeById(ID_STRING);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteEventType_returnsNotContentIfNothingWasDeleted() {
        given(mockRegistrar.deleteEventTypeById(ID_STRING)).willReturn(null);
        Response response = service.deleteEventType(ID_STRING);

        verify(mockRegistrar).deleteEventTypeById(ID_STRING);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateEventType_returnsExpected() {
        final String eventTypeId = "42";

        given(dao.getById(eventTypeId)).willReturn(mockEventType);
        given(dao.updateEventType(eventTypeId, NAME, DESCRIPTION, CONFIG)).willReturn(mockEventType);

        Response response = service.updateEventType(eventTypeId, VALID_JSON_STRING);

        verify(dao).updateEventType(eventTypeId, NAME, DESCRIPTION, CONFIG);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}
