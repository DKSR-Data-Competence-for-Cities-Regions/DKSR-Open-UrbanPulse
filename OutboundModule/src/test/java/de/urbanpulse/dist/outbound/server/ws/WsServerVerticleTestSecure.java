package de.urbanpulse.dist.outbound.server.ws;

import static de.urbanpulse.dist.outbound.server.ws.WsServerVerticle.SETUP_ADDRESS;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebsocketRejectedException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class WsServerVerticleTestSecure {

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
    private static final Logger LOGGER = LoggerFactory.getLogger(WsServerVerticleTestSecure.class);

    private MultiMap clientHeaders = MultiMap.caseInsensitiveMultiMap();

    WsServerTestVerticle ws = new WsServerTestVerticle();
    private String deplId;

    @Mock
    private DefaultSecurityManager secureSecurityManager;

    @Mock
    private Subject mockedSubject;

    public WsServerVerticleTestSecure() throws IOException {
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
        when(secureSecurityManager.createSubject(any())).thenReturn(mockedSubject);
        Mockito.doThrow(new VertxException("NO NO")).when(secureSecurityManager).authenticate(any());


        SecurityUtils.setSecurityManager(secureSecurityManager);

        clientHeaders = MultiMap.caseInsensitiveMultiMap();
        vertx = Vertx.vertx();
        config = readConfig();
        DeploymentOptions options = new DeploymentOptions().setWorker(true).setConfig(config.getJsonObject("wsServerConfig"));
        Async async = context.async();
        wsServerVerticleInstance = spy(new WsServerVerticle());
        vertx.deployVerticle(wsServerVerticleInstance, options, (res) -> {
            if (res.succeeded()) {
                LOGGER.info(WsServerVerticleTestSecure.class.getName() + " deployment id = " + res.result());
                deplId = res.result();
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
    public void bug7991_handleWebSocketConnection_returns401_ifNotUnauthorized(TestContext context) {
        Async async = context.async();
        boolean isEncrypted = false;

        HttpClient wsClient = createWsClient(TRUST_ALL, "127.0.0.1", isEncrypted ? PORT_SECURE : PORT_INSECURE, isEncrypted);

        wsClient.websocketStream("/OutboundInterfaces/outbound/" + STATEMENT_SECURE).
                exceptionHandler(t -> {
                    context.assertTrue(t instanceof WebsocketRejectedException);
                    context.assertEquals(401, ((WebsocketRejectedException) t).getStatus());
                    wsClient.close();
                    async.complete();
                }).handler(ws -> {
            wsClient.close();
            context.fail("Should not be called");
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
