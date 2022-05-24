package de.urbanpulse.dist.outbound.client;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpTargetQueueHandlerTest {

    private HttpClientMock client;

    @Before
    public void setUp() {
        client = new HttpClientMock();
    }

    @Test
    public void testHandleBasicAuthMethod() throws URISyntaxException {
        String credentials = "{\"authMethod\":\"BASIC\", \"user\":\"foo\", \"password\":\"bar\"}";
        HttpTargetQueueHandler httpTargetQueueHandler = new HttpTargetQueueHandler(client, new URI("dummy"), new JsonObject(credentials));
        httpTargetQueueHandler.handle(Collections.singletonList(new JsonObject()));

        assertTrue(((HttpClientRequestMock) client.getRequest()).isEndCalled());
        MultiMap headers = client.getRequest().headers();
        assertNotNull(headers.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testHandleUnknownAuthMethod() throws URISyntaxException {
        String credentials = "{\"authMethod\":\"PASCAL\", \"user\":\"foo\", \"password\":\"bar\"}";
        HttpTargetQueueHandler httpTargetQueueHandler = new HttpTargetQueueHandler(client, new URI("dummy"), new JsonObject(credentials));
        httpTargetQueueHandler.handle(Collections.singletonList(new JsonObject()));

        assertFalse(((HttpClientRequestMock) client.getRequest()).isEndCalled());
        MultiMap headers = client.getRequest().headers();
        assertNull(headers.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testBackwardsCompatibility() throws URISyntaxException {
        String credentials = "{\"hmacKey\":\"dbddhkpsav\"}";
        HttpTargetQueueHandler httpTargetQueueHandler = new HttpTargetQueueHandler(client, new URI("dummy"), new JsonObject(credentials));
        httpTargetQueueHandler.handle(Collections.singletonList(new JsonObject()));

        assertTrue(((HttpClientRequestMock) client.getRequest()).isEndCalled());
        MultiMap headers = client.getRequest().headers();
        assertNull(headers.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testInvalidCredentials() throws URISyntaxException {
        String credentials = "{\"banana\":\"bapple\", \"foo\":\"bar\", \"bla\":\"blub\"}";
        HttpTargetQueueHandler httpTargetQueueHandler = new HttpTargetQueueHandler(client, new URI("dummy"), new JsonObject(credentials));
        httpTargetQueueHandler.handle(Collections.singletonList(new JsonObject()));

        assertFalse(((HttpClientRequestMock) client.getRequest()).isEndCalled());
        MultiMap headers = client.getRequest().headers();
        assertNull(headers.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testQueryParameterUsage_bug10724() throws URISyntaxException {
        String credentials = "{\"banana\":\"bapple\", \"foo\":\"bar\", \"bla\":\"blub\"}";
        HttpClient clientMock = Mockito.mock(HttpClientMock.class);
        HttpClientRequest requestMock = Mockito.mock(HttpClientRequest.class);
        given(requestMock.putHeader(eq(HttpHeaders.CONTENT_TYPE), eq("application/json"))).willReturn(requestMock);
        given(clientMock.putAbs(any(), any())).willReturn(requestMock);

        HttpTargetQueueHandler httpTargetQueueHandler = new HttpTargetQueueHandler(clientMock, new URI("http://foo/bar?query=true"), new JsonObject(credentials));
        List<JsonObject> events = new ArrayList<>();
        events.add(new JsonObject());
        httpTargetQueueHandler.handle(events);
        verify(clientMock, Mockito.times(1)).putAbs(eq("http://foo/bar?query=true"), any());


    }
}
