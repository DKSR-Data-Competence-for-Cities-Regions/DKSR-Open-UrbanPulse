package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.auth.upsecurityrealm.shiro.UnsecurityManager;

import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.*;
import static de.urbanpulse.dist.outbound.server.ws.WsServerVerticle.SETUP_ADDRESS;
import de.urbanpulse.dist.util.UpdateListenerConfig;


import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.ext.auth.User;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.shiro.SecurityUtils;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class WsServerVerticleTest {

    private static final String SID_KEY = "SID";
    private static final String HOST = "localhost";
    private static final String STATEMENT_SECURE = "StatementSecure";
    private static final String STATEMENT_INSECURE = "StatementInsecure";
    private static final String STATEMENT_OTHERHOST = "StatementOtherHostname";
    private final int PORT_SECURE;
    private final int PORT_INSECURE;
    private static final boolean TRUST_ALL = true;
    private static final String EMPTY_STRING_FOR_UNUSED_KEYSTORE = "";

    WsServerVerticle wsServerVerticleInstance;

    private static Vertx vertx;
    private static JsonObject config;
    private static final Logger LOGGER = LoggerFactory.getLogger(WsServerVerticleTest.class);

    private MultiMap clientHeaders = MultiMap.caseInsensitiveMultiMap();

    WsServerTestVerticle ws = new WsServerTestVerticle();

    public WsServerVerticleTest() throws IOException {
        PORT_SECURE = getAvailablePort();
        PORT_INSECURE = getAvailablePort();
    }

    private int getAvailablePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    @Before
    public void setUp(TestContext context) {
        MockitoAnnotations.initMocks(this);
        SecurityUtils.setSecurityManager(new UnsecurityManager());
        clientHeaders = MultiMap.caseInsensitiveMultiMap();
        vertx = Vertx.vertx();
        config = readConfig();
        DeploymentOptions options = new DeploymentOptions().setWorker(true).setConfig(config.getJsonObject("wsServerConfig"));
        Async async = context.async();
        wsServerVerticleInstance = spy(new WsServerVerticle());
        vertx.deployVerticle(wsServerVerticleInstance, options, (res) -> {
            if (res.succeeded()) {
                LOGGER.info(WsServerVerticleTest.class.getName() + " deployment id = " + res.result());

                DeliveryOptions delopts = new DeliveryOptions();
                delopts.setSendTimeout(1000);

                List<JsonObject> generatedRegistrationJson = generateRegistrationJson();
                AtomicInteger successCounter = new AtomicInteger();
                generatedRegistrationJson.forEach(j -> {
                    vertx.eventBus().send(SETUP_ADDRESS, j, delopts, replyHandler -> {
                        if (replyHandler.failed()) {
                            context.fail(res.cause());
                        } else {
                            if (successCounter.incrementAndGet() == generatedRegistrationJson.size()) {
                                async.complete();
                            }
                        }
                    });
                });
            } else {
                context.fail(res.cause());
            }
        });

        clientHeaders.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(("foo:bar").getBytes()));

    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        LOGGER.info("tearing Down after integration test");
        vertx.close(a -> {
            async.complete();
        });
    }

    @Test
    public void test_secureRequestSecureDBEntry_success(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = true;
        HttpClient wsClient = createWsClient(TRUST_ALL, HOST, isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_SECURE, clientHeaders, wsConnectSuccess -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_SECURE).size() == 1);
                wsClient.close();
                async.complete();
            });
        }, wsConnectFailure -> {
            wsClient.close();
            context.fail();
            async.complete();
        });
    }

    @Test
    public void test_secureRequestInsecureDBEntry_failure(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = true;
        HttpClient wsClient = createWsClient(TRUST_ALL, HOST, isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_INSECURE, wsConnectSuccess -> {
            wsClient.close();
            context.fail();
        }, wsConnectFailure -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().isEmpty());
                wsClient.close();
                async.complete();
            });
        });
    }

    @Test
    public void test_insecureRequestSecureDBEntry_failure(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = false;
        HttpClient wsClient = createWsClient(TRUST_ALL, HOST, isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_SECURE, wsConnectSuccess -> {
            wsClient.close();
            context.fail();
        }, wsConnectFailure -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().isEmpty());
                wsClient.close();
                async.complete();
            });
        });
    }

    @Test
    public void test_insecureRequestInsecureDBEntry_success(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = false;
        HttpClient wsClient = createWsClient(TRUST_ALL, HOST, isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_INSECURE, clientHeaders, wsConnectSuccess -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_INSECURE).size() == 1);
                wsClient.close();
                async.complete();
            });
        }, wsConnectFailure -> {
            wsClient.close();
            context.fail();
        });
    }

    @Test
    public void test_nonmatchingexternalhost_success(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = false;
        HttpClient wsClient = createWsClient(TRUST_ALL, "127.0.0.1", isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_INSECURE, clientHeaders, wsConnectSuccess -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_INSECURE).size() == 1);
                wsClient.close();
                async.complete();
            });
        }, wsConnectFailure -> {
            wsClient.close();
            context.fail();
        });
    }

    @Test
    public void test_nonmatchingexternalhostinDB_success(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = false;
        HttpClient wsClient = createWsClient(TRUST_ALL, "127.0.0.1", isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocket("/OutboundInterfaces/outbound/" + STATEMENT_OTHERHOST, clientHeaders, wsConnectSuccess -> {
            vertx.setTimer(100, l -> {
                context.assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_OTHERHOST).size() == 1);
                wsClient.close();
                async.complete();
            });
        }, wsConnectFailure -> {
            wsClient.close();
            context.fail();
        });
    }

    @Test
    public void test_registerUpdateListener_statement_not_matching_target(TestContext context) {
        String otherStatement = "otherStatement";
        String target = "wss://localhost:" + PORT_SECURE + "/OutboundInterfaces/outbound/statement";
        UpdateListenerConfig ulConfig = new UpdateListenerConfig("dumyId", otherStatement, target);
        try {
            wsServerVerticleInstance.registerUpdateListener(ulConfig);
            assertTrue("Should not get here!", false);
        } catch (Exception ex) {
            String msg = String.format("ws server target has to match the statement name. target: [%s], statementName: [%s]", target, otherStatement);
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void test_registerUpdateListener_unsupported_path(TestContext context) {
        String statement = "statement";
        String target = "wss://localhost:" + PORT_SECURE + "/unsupported/" + statement;
        UpdateListenerConfig ulConfig = new UpdateListenerConfig("dumyId", statement, target);
        try {
            wsServerVerticleInstance.registerUpdateListener(ulConfig);
            assertTrue("Should not get here!", false);
        } catch (Exception ex) {
            String msg = "ws server target [" + target + "] unsupported";
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void test_registerUpdateListener_invalid_target_uri(TestContext context) {
        String statement = "statement";
        String target = null;
        UpdateListenerConfig ulConfig = new UpdateListenerConfig("dumyId", statement, target);
        try {
            wsServerVerticleInstance.registerUpdateListener(ulConfig);
            assertTrue("Should not get here!", false);
        } catch (Exception ex) {
            String msg = "ws server target [" + target + "] malformed URL";
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void test_registerUpdateListener_already_registered(TestContext context) {
        String target = "wss://localhost:" + PORT_SECURE + "/OutboundInterfaces/outbound/" + STATEMENT_SECURE;
        UpdateListenerConfig ulConfig = new UpdateListenerConfig("dumyId", STATEMENT_SECURE, target);
        try {
            wsServerVerticleInstance.registerUpdateListener(ulConfig);
            assertTrue("Should not get here!", false);
        } catch (Exception ex) {
            String msg = "update listener for statement [" + STATEMENT_SECURE + "] already registered";
            assertTrue(ex instanceof IllegalStateException);
            assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void test_reset(TestContext context) {
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, new ArrayList<>());
        assertFalse(wsServerVerticleInstance.getStatementToWsSessionMap().isEmpty());
        wsServerVerticleInstance.reset();
        assertTrue(wsServerVerticleInstance.getStatementToWsSessionMap().isEmpty());
    }

    @Test
    public void test_trackEvent(TestContext context) {
        Async async = context.async();
        JsonObject expected = new JsonObject("{\"TestEvent\":{\"type\":\"message_event\",\"count\":1,\"stringParameters\":{\"SID\":\"theSID\",\"streamName\":\"StatementSecure\"}}}");
        given(wsServerVerticleInstance.config()).willReturn(new JsonObject().put("enableMetrics", true));

    }

    @Test
    public void test_handle_event() {
        String sid_1 = "sid_1";
        JsonObject event_1 = new JsonObject().put(SID_KEY, sid_1);

        String sid_2 = "sid_2";
        JsonObject event_2 = new JsonObject().put(SID_KEY, sid_2);

        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        doReturn(false).when(webSocket).writeQueueFull();
        doReturn(new SocketAddressImpl(0, "dummyHost")).when(webSocket).remoteAddress();

        User user = Mockito.mock(User.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);
        List<ServerWebSocketWrapper> socketWrapperList = Arrays.asList(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_1);
        verify(user, times(1)).isAuthorized(eq("sensor:" + sid_1 + ":livedata:read"), any());

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_2);
        verify(user, times(1)).isAuthorized(eq("sensor:" + sid_2 + ":livedata:read"), any());

        wsServerVerticleInstance.getStatementToWsSessionMap().clear();
    }

    @Test
    public void test_remove_dead_session() {
        User user = Mockito.mock(User.class);
        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);

        List<ServerWebSocketWrapper> socketWrapperList = new ArrayList<>();
        socketWrapperList.add(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);
        Assert.assertEquals(1, wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_SECURE).size());

        doThrow(new RuntimeException("This is thrown on purpose!")).when(webSocket).writeQueueFull();
        doReturn(new SocketAddressImpl(0, "dummyHost")).when(webSocket).remoteAddress();

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, new JsonObject());

        //should be removed now
        Assert.assertEquals(0, wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_SECURE).size());
        wsServerVerticleInstance.getStatementToWsSessionMap().clear();
    }

    @Test
    public void test_unregisterUpdateListener() {
        User user = Mockito.mock(User.class);
        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);

        List<ServerWebSocketWrapper> socketWrapperList = new ArrayList<>();
        socketWrapperList.add(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);
        Assert.assertEquals(1, wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_SECURE).size());

        UpdateListenerConfig ulConfig = new UpdateListenerConfig("dummyId", STATEMENT_SECURE, "dummyHost");
        wsServerVerticleInstance.unregisterUpdateListener(ulConfig);

        //should be removed now
        Assert.assertEquals(null, wsServerVerticleInstance.getStatementToWsSessionMap().get(STATEMENT_SECURE));
        wsServerVerticleInstance.getStatementToWsSessionMap().clear();
    }

    @Test
    public void test_eventTypePermissions(){
        String eventType_1 = "eventType_1";
        JsonObject event_1 = new JsonObject().put("_headers", new JsonObject().put("eventType", eventType_1));

        String eventType_2 = "eventType_2";
        JsonObject event_2 = new JsonObject().put("_headers", new JsonObject().put("eventType", eventType_2));

        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        doReturn(false).when(webSocket).writeQueueFull();
        doReturn(new SocketAddressImpl(0, "dummyHost")).when(webSocket).remoteAddress();

        User user = Mockito.mock(User.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);
        List<ServerWebSocketWrapper> socketWrapperList = Arrays.asList(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_1);
        verify(user, times(1)).isAuthorized(eq("eventtype:" + eventType_1 + ":livedata:read"), any());

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_2);
        verify(user, times(1)).isAuthorized(eq("eventtype:" + eventType_2 + ":livedata:read"), any());

        wsServerVerticleInstance.getStatementToWsSessionMap().clear();

    }

    @Test
    public void test_checkPermissionsInHandleEventBothSucceed(){
        String eventType_1 = "eventType_1";
        JsonObject event_1 = new JsonObject().put("_headers", new JsonObject().put("eventType", eventType_1));

        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        doReturn(false).when(webSocket).writeQueueFull();
        doReturn(new SocketAddressImpl(0, "dummyHost")).when(webSocket).remoteAddress();

        User user = Mockito.mock(User.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);
        List<ServerWebSocketWrapper> socketWrapperList = Arrays.asList(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);
        doReturn(Future.succeededFuture()).when(wsServerVerticleInstance).checkPermissionsOnEventType(any(), any());
        doReturn(Future.succeededFuture()).when(wsServerVerticleInstance).checkPermissionsOnSID(any(), any());

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_1);
        verify(wsServerVerticleInstance, times(1)).handleAuthorizedResult(any(), any(), anyString());
    }

    @Test
    public void test_checkPermissionsInHandleEventBothFail(){
        String eventType_1 = "eventType_1";
        JsonObject event_1 = new JsonObject().put("_headers", new JsonObject().put("eventType", eventType_1));

        ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
        doReturn(false).when(webSocket).writeQueueFull();
        doReturn(new SocketAddressImpl(0, "dummyHost")).when(webSocket).remoteAddress();

        User user = Mockito.mock(User.class);
        ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(webSocket, user);
        List<ServerWebSocketWrapper> socketWrapperList = Arrays.asList(wsWrapper);
        wsServerVerticleInstance.getStatementToWsSessionMap().put(STATEMENT_SECURE, socketWrapperList);
        doReturn(Future.failedFuture("Broken")).when(wsServerVerticleInstance).checkPermissionsOnEventType(any(), any());
        doReturn(Future.failedFuture("Broken")).when(wsServerVerticleInstance).checkPermissionsOnSID(any(), any());

        wsServerVerticleInstance.handleEvent(STATEMENT_SECURE, event_1);
        verify(wsServerVerticleInstance, times(0)).handleAuthorizedResult(any(), any(), anyString());
    }

    @Test
    public void test_checkUserSIDPermissionsSuccess(TestContext context){
        Async async = context.async();
        User user = Mockito.mock(User.class);
        doAnswer((Answer<Void>) invocation -> {
            String authority = invocation.getArgument(0);
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture(String.format(PERMISSION_SENSOR_SID_LIVEDATA_READ_TEMPLATE, "A").equals(authority)));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        JsonObject testEvent = new JsonObject().put("SID", "A");

        wsServerVerticleInstance.checkPermissionsOnSID(testEvent, user).onComplete(hndlr -> {
           context.assertTrue(hndlr.succeeded());
           async.complete();
        });
    }

    @Test
    public void test_checkUserEventTypePermissionsSuccess(TestContext context){
        Async async = context.async();
        User user = Mockito.mock(User.class);
        doAnswer((Answer<Void>) invocation -> {
            String authority = invocation.getArgument(0);
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture(String.format(PERMISSION_EVENTTYPE_LIVEDATA_READ_TEMPLATE, "A").equals(authority)));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        JsonObject testEvent = new JsonObject().put("_headers", new JsonObject().put("eventType", "A"));

        wsServerVerticleInstance.checkPermissionsOnEventType(testEvent, user).onComplete(hndlr -> {
            context.assertTrue(hndlr.succeeded());
            async.complete();
        });
    }

    private HttpClient createWsClient(boolean trustAll, String host, int port, boolean encrypted) {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(host);
        options.setDefaultPort(port);
        options.setSsl(encrypted);
        options.setTrustAll(trustAll);
        options.setConnectTimeout(10000);
        return vertx.createHttpClient(options);
    }

    private List<JsonObject> generateRegistrationJson() {
        JsonObject secureReg = new JsonObject("{\"register\":{\"statementName\":\"" + STATEMENT_SECURE + "\",\"id\":\"6\",\"credentials\":{\"hmacKey\":\"UNUSED_HMAC\"},\"target\":\"wss://localhost:" + PORT_SECURE + "/OutboundInterfaces/outbound/" + STATEMENT_SECURE + "\"}}");
        JsonObject insecureReg = new JsonObject("{\"register\":{\"statementName\":\"" + STATEMENT_INSECURE + "\",\"id\":\"6\",\"credentials\":{\"hmacKey\":\"UNUSED_HMAC\"},\"target\":\"ws://localhost:" + PORT_INSECURE + "/OutboundInterfaces/outbound/" + STATEMENT_INSECURE + "\"}}");
        JsonObject insecureRegNonMatchingExtHost = new JsonObject("{\"register\":{\"statementName\":\"" + STATEMENT_OTHERHOST + "\",\"id\":\"6\",\"credentials\":{\"hmacKey\":\"UNUSED_HMAC\"},\"target\":\"ws://randomhost:" + PORT_INSECURE + "/OutboundInterfaces/outbound/" + STATEMENT_OTHERHOST + "\"}}");

        List<JsonObject> regList = new LinkedList<>();
        regList.add(secureReg);
        regList.add(insecureReg);
        regList.add(insecureRegNonMatchingExtHost);

        return regList;
    }

    private JsonObject readConfig() {
        return new JsonObject("{\n"
                + "    \"outboundDestination\": \"theOutbound\",\n"
                + "    \"testNoUpdateListeners\": false,\n"
                + "    \"clientConfig\": {\n"
                + "        \"credentials\": {\n"
                + "            \"basicAuthUsername\": \"demo\",\n"
                + "            \"basicAuthPassword\": \"review\"\n"
                + "        },\n"
                + "        \"trustAll\": true,\n"
                + "        \"queueWorkerCount\": 1,\n"
                + "        \"queueBatchSize\": 2000,\n"
                + "        \"queueCapacity\": 10000\n"
                + "    },\n"
                + "    \"wsServerConfig\": {\n"
                + "        \"credentials\": {\n"
                + "            \"foo\": \"$2a$10$1Ahh5qFNMdMIwRRbqRK72.OhVFgMT8jQZtyu3M0t.SYxs1H.tTKRy\"\n"
                + "        },\n"
                + "        \"host\": \"localhost\",\n"
                + "        \"externalHost\": \"localhost\",\n"
                + "        \"portSecure\": " + PORT_SECURE + ",\n"
                + "        \"portInsecure\": " + PORT_INSECURE + ",\n"
                + "        \"encrypt\": false,\n"
                + "        \"basePathWithLeadingSlashOnly\": \"/OutboundInterfaces/outbound\",\n"
                + "        \"comment\":\"OutboundInterfaces like in UP1\",\n"
                + "        \"keystore\": \"" + EMPTY_STRING_FOR_UNUSED_KEYSTORE + "\",\n"
                + "        \"keystorePassword\": \"changeit\",\n"
                + "        \"cipherSuites\": [\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA\"\n"
                + "        ]\n"
                + "    },\n"
                + "    \"historicalDataRestConfig\": {\n"
                + "        \"batchSize\": 100,\n"
                + "        \"sensorInformation\": {\n"
                + "        },\n"
                + "        \"users\": {\n"
                + "            \"demo\": {\n"
                + "                \"logAPIUsage\": true,\n"
                + "                \"password\": \"review\",\n"
                + "                \"allowedSids\": [\n"
                + "                ],\n"
                + "                \"defaultFilters\": {\n"
                + "                    \"requestFilters\": {\n"
                + "                        \"maxIntervalSizeInMinutes\": 100080\n"
                + "                    },\n"
                + "                    \"eventFilters\": {\n"
                + "                        \"eventParameterExcludeFilter\": [\n"
                + "                        ]\n"
                + "                    }\n"
                + "                },\n"
                + "                \"sidFilters\": {\n"
                + "                }\n"
                + "            }\n"
                + "        },\n"
                + "        \"port\": 4443,\n"
                + "        \"encrypt\": true,\n"
                + "        \"keystore\": \"keystore.jks\",\n"
                + "        \"keystorePassword\": \"changeit\",\n"
                + "        \"cipherSuites\": [\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA\",\n"
                + "            \"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA\",\n"
                + "            \"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA\"\n"
                + "        ]\n"
                + "    },\n"
                + "    \"loggingConfig\": {\n"
                + "        \"level\": \"INFO\",\n"
                + "        \"handlers\": \"java.util.logging.FileHandler, java.util.logging.ConsoleHandler\",\n"
                + "        \"pattern\": \"%t/Outboundv%g.log\",\n"
                + "        \"patternInfo\": \"%t is the system TEMP folder, %g is the generation number to distinguish between rotating logs.\",\n"
                + "        \"limit\": \"1024000\",\n"
                + "        \"count\": \"5\",\n"
                + "        \"formatter\": \"java.util.logging.SimpleFormatter\"\n"
                + "    }\n"
                + "}");
    }
}
