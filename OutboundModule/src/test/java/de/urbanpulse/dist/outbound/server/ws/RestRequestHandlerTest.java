package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.dist.util.UpdateListenerConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class RestRequestHandlerTest {

    private RestRequestHandler requestHandler;

    private WsServerTargetMatcher matcherMock;
    private Map<String, Set<UpdateListenerConfig>> statementToListenerMapMock;

    private RoutingContext routingContextMock;
    private HttpServerRequest requestMock;
    private HttpServerResponse responseMock;

    @Before
    public void setUp() throws URISyntaxException {
        matcherMock = Mockito.mock(WsServerTargetMatcher.class);
        statementToListenerMapMock = Mockito.mock(Map.class);
        requestHandler = new RestRequestHandler(matcherMock, new URI("http://localhost:3120/test"), statementToListenerMapMock);

        initRoutingContextMock();

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of handle method, of class RestRequestHandler.
     */
    @Test
    public void test_handle_not_GET() {
        when(requestMock.method()).thenReturn(HttpMethod.POST);

        requestHandler.handle(routingContextMock);
        verify(responseMock, times(1)).setStatusCode(eq(405));
    }

    @Test
    public void test_handle_not_haveMatchingTargetForStatement() {
        when(requestMock.method()).thenReturn(HttpMethod.GET);
        when(statementToListenerMapMock.containsKey(anyString())).thenReturn(Boolean.FALSE);

        requestHandler.handle(routingContextMock);
        verify(responseMock, times(1)).setStatusCode(eq(404));
    }

    @Test
    public void test_handle_haveMatchingTargetForStatement() {
        when(requestMock.method()).thenReturn(HttpMethod.GET);
        when(statementToListenerMapMock.containsKey(anyString())).thenReturn(Boolean.TRUE);
        when(matcherMock.extractStatement(anyString(), anyString())).thenReturn("statement");

        requestHandler.handle(routingContextMock);
        verify(responseMock, times(1)).setStatusCode(eq(200));
    }

     @Test
    public void test_handle_valid_filename_ws_de_html() {
        when(requestMock.method()).thenReturn(HttpMethod.GET);
        when(statementToListenerMapMock.containsKey(anyString())).thenReturn(Boolean.TRUE);
        when(matcherMock.extractStatement(anyString(), anyString())).thenReturn("statement");
        when(requestMock.getHeader("Accept-Language")).thenReturn("de");

        requestHandler.handle(routingContextMock);
        verify(responseMock, times(1)).setStatusCode(eq(200));

        ArgumentCaptor<Buffer> bodyCapture = ArgumentCaptor.forClass(Buffer.class);
        verify(responseMock, times(1)).end(bodyCapture.capture());
        assertTrue("Should be german version!", bodyCapture.getValue().toString().contains("Echtzeitdaten"));
    }


    private void initRoutingContextMock() {
        routingContextMock = Mockito.mock(RoutingContext.class);
        requestMock = Mockito.mock(HttpServerRequest.class);
        responseMock = Mockito.mock(HttpServerResponse.class);

        when(routingContextMock.request()).thenReturn(requestMock);
        when(requestMock.response()).thenReturn(responseMock);
        when(requestMock.path()).thenReturn("path");
        when(responseMock.setStatusCode(anyInt())).thenReturn(responseMock);
    }

}
