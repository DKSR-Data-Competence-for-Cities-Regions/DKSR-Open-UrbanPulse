package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.FailedToPersistStatementException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualSensorsDependencyCreatorTest {

    @InjectMocks
    private VirtualSensorsDependencyCreator creator;

    @Mock
    private StatementManagementDAO mockStatementDAO;

    @Mock
    private EventTypeManagementDAO mockEventTypeDAO;

    @Mock
    private EventProcessorWrapper mockEventProcessor;

    private static final String RESULT_STATEMENT_ID = "4711";
    private static final String RESULT_STATEMENT_NAME = "myLittleResultStatement";
    private static final String RESULT_STATEMENT_QUERY = "myLittleResultQuery";

    private static final String RESULT_EVENTTYPE_ID = "4712";

    private static final String STATEMENT_ID = "13";
    private static final String STATEMENT_NAME = "myLittleStatement";
    private static final String STATEMENT_QUERY = "myLittleQuery";
    private static final JsonObject STATEMENT
            = Json.createObjectBuilder().add("name", STATEMENT_NAME).add("query", STATEMENT_QUERY).build();

    private static final JsonArray EXPECTED_STATEMENT_IDS = Json.createArrayBuilder().add(STATEMENT_ID).build();

    private static final String EVENT_TYPE_ID = "2001";
    private static final String EVENT_TYPE_NAME = "myLitteEventType";
    private static final JsonObject EVENT_TYPE_CONFIG = Json.createObjectBuilder().add("value", "string").add("timestamp", "java.util.Date").build();
    private static final JsonObject EVENT_TYPE_DESCRIPTION = Json.createObjectBuilder().add("hello", "world").build();
    private static final JsonObject EVENT_TYPE
            = Json.createObjectBuilder().add("name", EVENT_TYPE_NAME).add("config", EVENT_TYPE_CONFIG).add("description", EVENT_TYPE_DESCRIPTION).build();

    private static final JsonArray EXPECTED_EVENT_TYPE_IDS = Json.createArrayBuilder().add(EVENT_TYPE_ID).build();

    private static final String VIRTUAL_SENSOR_ID = "Virtually anything";

    private List<JsonObject> eventTypes;
    private List<JsonObject> statements;

    @Mock
    private EventTypeTO mockEventTypeTO;

    @Mock
    private StatementTO mockStatementTO;

    @Mock
    private StatementTO mockResultStatementTO;

    @Before
    public void setUp() {
        eventTypes = new LinkedList<>();
        statements = new LinkedList<>();
        eventTypes.add(EVENT_TYPE);
        statements.add(STATEMENT);
    }

    @Test
    public void persistEventTypes_returnsExpected() {;
        given(mockEventTypeTO.getId()).willReturn(EVENT_TYPE_ID);
        given(mockEventTypeDAO.createEventType(EVENT_TYPE_NAME, EVENT_TYPE_DESCRIPTION.toString(), EVENT_TYPE_CONFIG.toString())).willReturn(mockEventTypeTO);

        JsonArray eventTypeIds = creator.persistEventTypes(eventTypes);

        assertEquals(EXPECTED_EVENT_TYPE_IDS, eventTypeIds);
    }

    @Test
    public void persistStatements_returnsExpected() throws FailedToPersistStatementException {
        given(mockStatementTO.getId()).willReturn(STATEMENT_ID);
        given(mockStatementDAO.createStatement(eq(STATEMENT_NAME), eq(STATEMENT_QUERY), any())).willReturn(mockStatementTO);

        JsonArray statementIds = creator.persistStatements(statements);

        assertEquals(EXPECTED_STATEMENT_IDS, statementIds);
    }

    @Test
    public void bulkRegisterWithEventProcessor_doesAtomicBulkRegistration() throws Exception {
        given(mockEventTypeDAO.getById(EVENT_TYPE_ID)).willReturn(mockEventTypeTO);
        given(mockEventTypeDAO.getById(RESULT_EVENTTYPE_ID)).willReturn(mockEventTypeTO);
        given(mockStatementDAO.getById(RESULT_STATEMENT_ID)).willReturn(mockResultStatementTO);
        given(mockStatementDAO.getById(STATEMENT_ID)).willReturn(mockStatementTO);
        given(mockStatementTO.getQuery()).willReturn(STATEMENT_QUERY);
        given(mockStatementTO.getName()).willReturn(STATEMENT_NAME);
        given(mockStatementTO.getId()).willReturn("AnId");
        given(mockResultStatementTO.getQuery()).willReturn(RESULT_STATEMENT_QUERY);
        given(mockResultStatementTO.getName()).willReturn(RESULT_STATEMENT_NAME);

        given(mockEventTypeTO.toJson()).willReturn(EVENT_TYPE);

        creator.bulkRegisterWithEventProcessor(VIRTUAL_SENSOR_ID, EXPECTED_EVENT_TYPE_IDS.toString(), RESULT_STATEMENT_ID, EXPECTED_STATEMENT_IDS.toString(), RESULT_EVENTTYPE_ID, new ArrayList<>());

        verify(mockEventProcessor).registerVirtualSensor(eq(VIRTUAL_SENSOR_ID), any(io.vertx.core.json.JsonArray.class), any(io.vertx.core.json.JsonArray.class), any(io.vertx.core.json.JsonObject.class), any(io.vertx.core.json.JsonObject.class));
    }

    @Test(expected = FailedToPersistStatementException.class)
    public void persistStatements_throwsDuplicateStatementNameException_onDuplicateNames() throws Exception {
        // Null is returned on duplicate statement names

        creator.persistStatements(statements);
    }

    @Test(expected = WrappedWebApplicationException.class)
    public void persistEventType_throwsWrappedWebApplicationExceptionIfEventTypeExists() {
        given(mockEventTypeDAO.eventTypeExists(EVENT_TYPE_NAME)).willReturn(Boolean.TRUE);
        creator.persistEventType(EVENT_TYPE);
    }

}
