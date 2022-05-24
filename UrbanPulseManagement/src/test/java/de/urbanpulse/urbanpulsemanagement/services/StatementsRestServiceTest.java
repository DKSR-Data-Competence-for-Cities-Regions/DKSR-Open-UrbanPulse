package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.StatementsRestFacade;
import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.transfer.UpdateListenersWrapperTO;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class StatementsRestServiceTest {

    private static final String AUTH_HEADER = "someValidAuthHeader";
    private static final URI ABSOLUTE_PATH = URI.create("https://foo.bar/some/absolute/path/with/https/");
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/statements/42");

    private static final String STATEMENT_ID = "42";
    private static final String STATEMENT_NAME = "test";
    private static final String RELATIVE_PATH_FOR_ROOT = "/statements";

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock(name = "context")
    private UriInfo mockContext;

    @Mock(name = "eventProcessor")
    EventProcessorWrapper eventProcessorMock;

    @Mock(name = "outboundInterfacesDao")
    private OutboundInterfacesManagementDAO outboundInterfacesDaoMock;

    @Mock(name = "statementDao")
    private StatementManagementDAO statementDaoMock;

    @InjectMocks
    private StatementsRestService service;

    @Mock
    private StatementTO statementTOMock;

    @Mock
    private UpdateListenerTO updateListenerTOMock;

    @Mock
    StatementsRestFacade statementsRestFacadeMock;

    @Mock
    List<UpdateListenerTO> updateListenerTOsMock;

    @Test
    public void testGetUpdateListeners_returnsExpected() throws Exception {
        List<UpdateListenerTO> listeners = new ArrayList<>();
        listeners.add(updateListenerTOMock);

        given(statementDaoMock.getById(eq(STATEMENT_ID))).willReturn(statementTOMock);
        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(eq(STATEMENT_ID))).willReturn(listeners);

        Response response = service.getUpdateListeners(STATEMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUpdateListeners_statementNoFound() throws Exception {
        Response response = service.getUpdateListeners("23");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetStatements_statementNoFound() throws Exception {

        Response response = service.getStatement("23");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetStatements_returnsExpected() throws Exception {
        List<StatementTO> statements = new ArrayList<>();
        statements.add(statementTOMock);

        given(statementDaoMock.getAll()).willReturn(statements);

        Response response = service.getStatements();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetStatement_returnsExpected() throws Exception {
        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);

        Response response = service.getStatement(STATEMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetStatement_notFound() throws Exception {
        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(null);

        Response response = service.getStatement(STATEMENT_ID);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateStatement_returnsExpected() throws Exception {
        StatementTO statement = new StatementTO();
        statement.setName("myname");
        statement.setQuery("where bla LIKE blubb");

        given(statementTOMock.getId()).willReturn("anId");
        given(statementDaoMock.createStatement(eq("myname"), eq("where bla LIKE blubb"), any())).willReturn(statementTOMock);

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = service.createStatement(statement, mockContext, statementsRestFacadeMock);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateStatement_incompleteJson_MissingName() throws Exception {
        StatementTO statement = new StatementTO();
        statement.setQuery("where bla LIKE blubb");

        Response response = service.createStatement(statement, mockContext, statementsRestFacadeMock);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void testCreateStatement_incompleteJson_MissingQuery() throws Exception {
        StatementTO statement = new StatementTO();
        statement.setName("myname");

        Response response = service.createStatement(statement, mockContext, statementsRestFacadeMock);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void testDeleteStatement_returnsExpected() throws Exception {
        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);
        given(statementTOMock.getName()).willReturn(STATEMENT_NAME);
        given(statementDaoMock.deleteById(STATEMENT_ID)).willReturn(STATEMENT_NAME);

        Response response = service.deleteStatement(STATEMENT_ID);

        verify(eventProcessorMock, times(1)).unregisterStatement(STATEMENT_NAME);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteStatement_statementDoesNotExist() throws Exception {
        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(null);

        Response response = service.deleteStatement(STATEMENT_ID);

        verify(eventProcessorMock, never()).unregisterStatement(STATEMENT_NAME);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteStatement_stillHasListeners() throws Exception {
        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(STATEMENT_ID)).willReturn(updateListenerTOsMock);
        given(updateListenerTOsMock.isEmpty()).willReturn(false);

        Response response = service.deleteStatement(STATEMENT_ID);

        verify(eventProcessorMock, never()).unregisterStatement(STATEMENT_NAME);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void regressionBug1606_registerUpdateListener_doesNotCreateListenerAndReturns400forInvalidId() throws Exception {
        UpdateListenerTO updateListener = new UpdateListenerTO();
        updateListener.setTarget("https://foo.bar/statements/23");
        AuthJsonTO authJsonTO = new AuthJsonTO();
        updateListener.setAuthJson(authJsonTO);
        StatementEntity statementEntity = new StatementEntity();

        final List<UpdateListenerTO> updateListenersTO = new ArrayList<>();

        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(STATEMENT_ID)).willReturn(updateListenersTO);
        given(outboundInterfacesDaoMock.isValidAuthMethod(authJsonTO)).willReturn(true);

        Response response = service.registerUpdateListener(STATEMENT_ID, updateListener, mockContext, StatementsRestFacade.class);

        Assert.assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        verify(outboundInterfacesDaoMock, never()).createUpdateListener(any(StatementEntity.class), anyString(), any());
    }

    @Test
    public void registerUpdateListener_RejectInvalidAuthMethod() throws Exception {
        UpdateListenerTO updateListener = new UpdateListenerTO();
        updateListener.setTarget("https://foo.bar/statements/23");
        AuthJsonTO authJsonTO = new AuthJsonTO("{\n"
                + "\"authMethod\" : \"PASCAL\",\n"
                + "\"user\": \"foo\",\n"
                + "\"password\":\"bar\"\n"
                + "}");
        updateListener.setAuthJson(authJsonTO);

        StatementEntity statementEntity = new StatementEntity();
        statementEntity.setId(STATEMENT_ID);
        statementEntity.setName(STATEMENT_NAME);

        final List<UpdateListenerTO> updateListenersTO = new ArrayList<>();

        Response response = service.registerUpdateListener(STATEMENT_ID, updateListener, mockContext, StatementsRestFacade.class);

        Assert.assertNotNull(response);
        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
        verify(outboundInterfacesDaoMock, never()).createUpdateListener(any(StatementEntity.class), anyString(), any());
    }

    @Test
    public void testRegisterUpdateListener_returnsExpected() throws Exception {
        final String targetUrl = "https://foo.bar/statements/23";

        StatementEntity statementEntity = new StatementEntity();
        statementEntity.setId(STATEMENT_ID);
        statementEntity.setName(STATEMENT_NAME);

        UpdateListenerTO updateListener = new UpdateListenerTO();
        updateListener.setTarget(targetUrl);
        AuthJsonTO authJsonTO = new AuthJsonTO();
        updateListener.setAuthJson(authJsonTO);

        final String otherTargetUrl = "https://foo.bar/statements/42";
        final List<UpdateListenerTO> updateListenerTOs = new ArrayList<>();

        updateListenerTOs.add(updateListenerTOMock);

        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(STATEMENT_ID)).willReturn(updateListenerTOs);
        given(outboundInterfacesDaoMock.isValidAuthMethod(authJsonTO)).willReturn(true);
        given(updateListenerTOMock.getTarget()).willReturn(otherTargetUrl);

        given(outboundInterfacesDaoMock.createUpdateListener(statementEntity, targetUrl, authJsonTO)).willReturn(updateListenerTOMock);
        given(updateListenerTOMock.getId()).willReturn("23");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        given(statementDaoMock.queryById(STATEMENT_ID)).willReturn(statementEntity);

        Response response = service.registerUpdateListener(STATEMENT_ID, updateListener, mockContext, StatementsRestFacade.class);

        Assert.assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterUpdateListener_targetMissing() throws Exception {
        UpdateListenerTO updateListener = new UpdateListenerTO();

        Response response = service.registerUpdateListener(STATEMENT_ID, updateListener, mockContext, StatementsRestFacade.class);

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void testRegisterUpdateListener_alreadyRegistered() throws Exception {
        final String targetUrl = "https://foo.bar/statements/23";

        UpdateListenerTO updateListener = new UpdateListenerTO();
        updateListener.setTarget(targetUrl);
        AuthJsonTO authJson = new AuthJsonTO();
        updateListener.setAuthJson(authJson);

        final String json = "{\"target\":\"https://foo.bar/statements/23\"}";
        final List<UpdateListenerTO> updateListenersTO = new ArrayList<>();

        updateListenersTO.add(updateListenerTOMock);

        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(STATEMENT_ID)).willReturn(updateListenersTO);
        given(outboundInterfacesDaoMock.isValidAuthMethod(authJson)).willReturn(true);
        given(updateListenerTOMock.getTarget()).willReturn(targetUrl);

        Response response = service.registerUpdateListener(STATEMENT_ID, updateListener, mockContext, StatementsRestFacade.class);

        Assert.assertNotNull(response);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetListener_returnsExpected() throws Exception {
        final String listenerId = "4711";

        given(outboundInterfacesDaoMock.getUpdateListenerById(listenerId)).willReturn(updateListenerTOMock);
        given(updateListenerTOMock.getStatementId()).willReturn(STATEMENT_ID);

        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);

        Response response = service.getListener(STATEMENT_ID, listenerId);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetListener_listenerNotFound() throws Exception {
        final String listenerId = "4711";

        given(outboundInterfacesDaoMock.getUpdateListenerById(listenerId)).willReturn(null);

        Response response = service.getListener(STATEMENT_ID, listenerId);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetListener_statementNotFound() throws Exception {
        final String listenerId = "4711";

        given(outboundInterfacesDaoMock.getUpdateListenerById(listenerId)).willReturn(updateListenerTOMock);
        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(null);

        Response response = service.getListener(STATEMENT_ID, listenerId);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRemoveListener_returnsExpected() throws Exception {
        final String listenerId = "4711";

        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);

        Response response = service.removeListener(STATEMENT_ID, listenerId);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRemoveListener_noStatementFound() throws Exception {
        final String listenerId = "4711";

        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(null);

        Response response = service.removeListener(STATEMENT_ID, listenerId);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUpdateListeners_passwordsAreCensored() throws Exception {
        AuthJsonTO authJson = new AuthJsonTO();
        authJson.setAuthMethod("Banana");
        authJson.setUser("Mary Poppins");
        authJson.setPassword("supercalifragilisticexpialidocious");

        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);
        given(updateListenerTOMock.getAuthJson()).willReturn(authJson);
        given(outboundInterfacesDaoMock.getUpdateListenersOfStatement(STATEMENT_ID)).willReturn(Collections.singletonList(updateListenerTOMock));

        Response response = service.getUpdateListeners(STATEMENT_ID);
        UpdateListenersWrapperTO entity = (UpdateListenersWrapperTO) response.getEntity();
        assertEquals(1, entity.getListeners().size());
        assertNull(entity.getListeners().get(0).getAuthJson().getPassword());
    }

    @Test
    public void testGetListener_passwordsAreCensored() throws Exception {
        final String listenerId = "4711";

        AuthJsonTO authJson = new AuthJsonTO();
        authJson.setAuthMethod("Banana");
        authJson.setUser("Mary Poppins");
        authJson.setPassword("supercalifragilisticexpialidocious");

        given(updateListenerTOMock.getAuthJson()).willReturn(authJson);
        given(updateListenerTOMock.getStatementId()).willReturn(STATEMENT_ID);
        given(outboundInterfacesDaoMock.getUpdateListenerById(listenerId)).willReturn(updateListenerTOMock);

        given(statementDaoMock.getById(STATEMENT_ID)).willReturn(statementTOMock);

        Response response = service.getListener(STATEMENT_ID, listenerId);

        UpdateListenerTO entity = (UpdateListenerTO) response.getEntity();
        assertNull(entity.getAuthJson().getPassword());
    }

}
