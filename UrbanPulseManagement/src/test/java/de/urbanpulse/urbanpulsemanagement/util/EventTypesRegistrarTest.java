package de.urbanpulse.urbanpulsemanagement.util;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import java.io.StringReader;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
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
public class EventTypesRegistrarTest {

    private static final String ID_STRING = "13";
    private static final String NAME = "myLittleEventType";
    private static final String CONFIG = "{}";
    private static final String DESCRIPTION = "{}";
    private static final String JSON_STRING = "{"
            + "\"config\":" + CONFIG + ","
            + "\"description\":" + DESCRIPTION + ","
            + "\"name\":\"" + NAME + "\""
            + "}";

    private static final JsonObject JSON_OBJECT = Json.createReader(new StringReader(JSON_STRING)).readObject();

    @InjectMocks
    private EventTypesRegistrar registrar;

    @Mock
    private EventProcessorWrapper mockEventProcessor;

    @Mock
    private EventTypeManagementDAO mockDao;

    @Mock
    private EventTypeTO mockEventType;

    @Mock
    private List<EventTypeTO> mockEventTypes;

    @Before
    public void setUp() throws Exception {
        given(mockEventType.getName()).willReturn(NAME);
    }

    @Test
    public void registerEventType_returnsPersistedTypeIfEprRegistrationSucceeds() {
        given(mockDao.createEventType(NAME, DESCRIPTION, CONFIG)).willReturn(mockEventType);

        assertSame(mockEventType, registrar.registerEventType(JSON_OBJECT));
    }

    @Test(expected = WrappedWebApplicationException.class)
    public void registerEventType_throwsWrappedWebApplicationExceptionIfEventTypeExists() {
        given(mockDao.eventTypeExists(NAME)).willReturn(Boolean.TRUE);
        registrar.registerEventType(JSON_OBJECT);
    }

    @Test
    public void getAllEvenTypes_returnsAllEventTypes() {
        given(mockDao.getAll()).willReturn(mockEventTypes);

        assertSame(mockEventTypes, registrar.getAllEventTypes());
    }

    @Test
    public void getEventTypeById_returnsEventType() {
        given(mockDao.getById(ID_STRING)).willReturn(mockEventType);

        assertSame(mockEventType, registrar.getEventTypeById(ID_STRING));
    }

    @Test
    public void deleteEventType_deletesFromDatabaseAndEprThenReturnsIdIfFound() throws Exception {
        given(mockDao.getById(ID_STRING)).willReturn(mockEventType);

        assertEquals(ID_STRING, registrar.deleteEventTypeById(ID_STRING));
        verify(mockDao).deleteById(ID_STRING);
        verify(mockEventProcessor).unregisterEventType(NAME);
    }

    @Test
    public void deleteEventType_returnsNull_ifNotFound() {
        given(mockDao.getById(ID_STRING)).willReturn(null);

        assertNull(registrar.deleteEventTypeById(ID_STRING));
    }

    @Test
    public void test_deleteEventTypeById_unregisterFromCEPFalse_doesNotUnregisterFromCEP() throws Exception {
        given(mockDao.getById(ID_STRING)).willReturn(mockEventType);
        String result = registrar.deleteEventTypeById(ID_STRING, false);
        assertEquals(ID_STRING, result);
        verify(mockEventProcessor, never()).unregisterEventType(anyString());
    }

    @Test
    public void test_deleteEventTypeById_unregisterFromCEPTrue_unregistersFromCEP() throws Exception {
        given(mockDao.getById(ID_STRING)).willReturn(mockEventType);
        String result = registrar.deleteEventTypeById(ID_STRING, true);
        assertEquals(ID_STRING, result);
        verify(mockEventProcessor, times(1)).unregisterEventType(anyString());
    }

    @Test
    public void test_deleteEventTypeById_default_unregistersFromCEP() throws Exception {
        given(mockDao.getById(ID_STRING)).willReturn(mockEventType);
        String result = registrar.deleteEventTypeById(ID_STRING);
        assertEquals(ID_STRING, result);
        verify(mockEventProcessor, times(1)).unregisterEventType(anyString());
    }
}
