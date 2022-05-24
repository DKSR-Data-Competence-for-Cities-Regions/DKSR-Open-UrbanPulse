package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.VirtualSensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.FailedToPersistStatementException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.VirtualSensorConfiguration;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import static org.junit.Assert.*;
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
public class VirtualSensorsInnerCreatorTest {

    @InjectMocks
    private VirtualSensorsCreator creator;

    @Mock
    private VirtualSensorManagementDAO mockDao;

    @Mock
    private VirtualSensorsDependencyCreator mockDependencyCreator;

    @Mock
    private UriInfo mockContext;

    @Mock
    private AbstractRestFacade mockRestFacade;

    @Mock
    private VirtualSensorConfiguration mockConfiguration;

    @Mock
    private List<JsonObject> mockEventTypes;

    @Mock
    private List<JsonObject> mockStatements;

    @Mock
    private JsonObject mockResultStatement;

    @Mock
    private StatementTO mockResultStatementTO;

    @Mock
    private VirtualSensorTO mockVirtualSensor;

    @Mock
    private JsonObject mockResultEventType;

    @Mock
    private EventTypeTO mockResultEventTypeTO;

    private static final String CATEGORY_ID = "13";
    private static final String SID = "2010";

    private static final URI EXPECTED_LOCATION = URI.create("https://some.where/else/in/equestria");

    private static final String RESULT_STATEMENT_ID = "42";
    private static final String RESULT_EVENTTYPE_ID = "4712";

    private static final JsonArray EVENT_TYPE_IDS_JSON_ARRAY = Json.createArrayBuilder().build();
    private static final String EVENT_TYPE_IDS_JSON = EVENT_TYPE_IDS_JSON_ARRAY.toString();

    private static final JsonArray STATEMENT_IDS_ARRAY = Json.createArrayBuilder().build();
    private final JsonObject DESCRIPTION = Json.createObjectBuilder().build();

    @Mock
    private UriBuilder mockUriBuilder;

    @Before
    public void setUp() throws ReferencedEntityMissingException, FailedToPersistStatementException {

        given(mockConfiguration.getDescription()).willReturn(DESCRIPTION);
        given(mockConfiguration.getEventTypes()).willReturn(mockEventTypes);
        given(mockConfiguration.getResultstatement()).willReturn(mockResultStatement);
        given(mockConfiguration.getResultEventType()).willReturn(mockResultEventType);
        given(mockConfiguration.getStatments()).willReturn(mockStatements);

        given(mockResultStatementTO.getId()).willReturn(RESULT_STATEMENT_ID);
        given(mockResultEventTypeTO.getId()).willReturn(RESULT_EVENTTYPE_ID);

        given(mockDependencyCreator.persistStatement(mockResultStatement)).willReturn(mockResultStatementTO);
        given(mockDependencyCreator.persistEventTypes(mockEventTypes)).willReturn(EVENT_TYPE_IDS_JSON_ARRAY);
        given(mockDependencyCreator.persistEventType(mockResultEventType)).willReturn(mockResultEventTypeTO);
        given(mockDependencyCreator.persistStatements(mockStatements)).willReturn(STATEMENT_IDS_ARRAY);

        given(mockDao.createVirtualSensorAndReplaceSidPlaceholder(CATEGORY_ID, RESULT_STATEMENT_ID,
                STATEMENT_IDS_ARRAY.toString(), DESCRIPTION.toString(), EVENT_TYPE_IDS_JSON, RESULT_EVENTTYPE_ID, new ArrayList<>())).willReturn(mockVirtualSensor);

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);
    }

    @Test
    public void createVirtualSensorFromConfig_returnsExpected() throws ReferencedEntityMissingException {
        when(mockVirtualSensor.getId()).thenReturn("anId");
        when(mockVirtualSensor.getSid()).thenReturn(SID);
        Response response = creator.createVirtualSensorFromConfig(CATEGORY_ID, mockConfiguration, mockContext, mockRestFacade);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(EXPECTED_LOCATION.toString(), response.getHeaderString("Location"));
    }

    @Test
    public void createVirtualSensorFromConfig_doesBulkRegistrationAfterAllPersistenceIsDone()
            throws Exception {
        when(mockVirtualSensor.getId()).thenReturn("anId");
        when(mockVirtualSensor.getSid()).thenReturn(SID);
        creator.createVirtualSensorFromConfig(CATEGORY_ID, mockConfiguration, mockContext, mockRestFacade);

        InOrder inOrder = inOrder(mockDao, mockDependencyCreator);
        inOrder.verify(mockDependencyCreator).persistEventTypes(mockEventTypes);
        inOrder.verify(mockDependencyCreator).persistStatement(mockResultStatement);
        inOrder.verify(mockDependencyCreator).persistStatements(mockStatements);
        inOrder.verify(mockDao).createVirtualSensorAndReplaceSidPlaceholder(CATEGORY_ID, RESULT_STATEMENT_ID,
                STATEMENT_IDS_ARRAY.toString(), DESCRIPTION.toString(), EVENT_TYPE_IDS_JSON, RESULT_EVENTTYPE_ID, new ArrayList<>());

        inOrder.verify(mockDependencyCreator).bulkRegisterWithEventProcessor(anyString(), eq(EVENT_TYPE_IDS_JSON), eq(RESULT_STATEMENT_ID),
                eq(STATEMENT_IDS_ARRAY.toString()), eq(RESULT_EVENTTYPE_ID), eq(new ArrayList<>()));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testCreateVirtualSensorFromConfig_AnyEventProcessorWrapperException_ShouldReturnHttp422WithErrorObject() throws Exception {
        List<io.vertx.core.json.JsonObject> errorList = new ArrayList<>();
        errorList.add(new io.vertx.core.json.JsonObject("{\"body\":{\"error\": \"statement malformed\"}}"));

        EventProcessorWrapperException epwe = new EventProcessorWrapperException("statement malformed", errorList);

        doThrow(epwe).when(mockDependencyCreator).bulkRegisterWithEventProcessor(anyString(), any(), any(), any(), any(), any());
        when(mockVirtualSensor.getId()).thenReturn("anId");
        when(mockVirtualSensor.getSid()).thenReturn("anId");

        Response response = creator.createVirtualSensorFromConfig(CATEGORY_ID, mockConfiguration, mockContext, mockRestFacade);

        assertEquals(422, response.getStatus());
        assertEquals(new io.vertx.core.json.JsonObject("{\"error\":[\"statement malformed\"]}").encodePrettily(), response.getEntity().toString());
    }

    @Test
    public void testCreateVirtualSensorFromConfig_DuplicateStatementName_ShouldThrowBadRequest() throws Exception {
        doThrow(FailedToPersistStatementException.class).when(mockDependencyCreator).persistStatements(any());
        try {
            creator.createVirtualSensorFromConfig(CATEGORY_ID, mockConfiguration, mockContext, mockRestFacade);
            fail("Expected WrappedWebApplicationException");
        } catch (WrappedWebApplicationException wwae) {
            assertTrue(wwae.getCause() instanceof ClientErrorException);
            ClientErrorException cee = (ClientErrorException) wwae.getCause();
            Response response = cee.getResponse();
            assertEquals(400, response.getStatus());
        }
    }
}
