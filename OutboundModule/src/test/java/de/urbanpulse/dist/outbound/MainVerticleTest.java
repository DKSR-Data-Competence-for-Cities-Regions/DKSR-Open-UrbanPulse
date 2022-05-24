package de.urbanpulse.dist.outbound;

import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.CommandResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private Vertx vertx;
    private MainVerticle mainVerticle;
    private JsonObject validConfig;
    private JsonObject upServiceConfig;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        mainVerticle = new MainVerticle();

        validConfig = new JsonObject("{\n"
                + "    \"MainVerticle\": {\n"
                + "        \"security\": {\n"
                + "            \"jdbc\": {\n"
                + "                \"driverClassName\": \"org.postgresql.Driver\",\n"
                + "                \"username\": \"urbanuser\",\n"
                + "                \"password\": \"dfgkdf93nv73n\",\n"
                + "                \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n"
                + "                \"maxActive\": 8,\n"
                + "                \"maxIdle\": 8,\n"
                + "                \"initialSize\": 1\n"
                + "            },\n"
                + "            \"keycloak\": {\n"
                + "              \"clientId\": \"urbanpulse\",\n"
                + "              \"secret\": \"<client-secert>\",\n"
                + "              \"apiBaseUrl\": \"http://localhost:9080/auth\",\n"
                + "              \"realm\": \"ui\"\n"
                + "            }\n"
                + "        }\n"
                + "    },\n"
                + "    \"logEventBusContent\": true,\n"
                + "    \"outboundDestination\": \"theOutbound\",\n"
                + "    \"testNoUpdateListeners\": false,\n"
                + "    \"hint\": \"sharedServerConfig may contain host,encrypt,keystore,keystorePassword and cipherSuites\",\n"
                + "    \"sharedServerConfig\": {\n"
                + "        \"host\": \"0.0.0.0\",\n"
                + "        \"encrypt\": true,\n"
                + "        \"keystore\": \"localhost_keystore.jks\",\n"
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
                + "    \"clientConfig\": {\n"
                + "        \"trustAll\": true,\n"
                + "        \"queueWorkerCount\": 1,\n"
                + "        \"queueBatchSize\": 2000,\n"
                + "        \"queueCapacity\": 10000\n"
                + "    },\n"
                + "    \"wsServerConfig\": {\n"
                + "        \"externalHost\": \"localhost\",\n"
                + "        \"portSecure\": 3210,\n"
                + "        \"portInsecure\": 3211,\n"
                + "        \"basePathWithLeadingSlashOnly\": \"/OutboundInterfaces/outbound\",\n"
                + "        \"enableMetrics\": false,\n"
                + "        \"comment\": \"OutboundInterfaces like in UP1\"\n"
                + "    },\n"
                + "    \"historicalDataRestConfig\": {\n"
                + "        \"httpVerticleInstances\": 2,\n"
                + "        \"batchSize\": 100,\n"
                + "        \"sensorInformation\": {\n"
                + "            \"451\": \"OWM example sensor\"\n"
                + "        },\n"
                + "        \"rules\": {\n"
                + "            \"default_rule\": {\n"
                + "                \"logAPIUsage\": true,\n"
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
                + "        \"roles_to_rules\": {\n"
                + "            \"historic_data_reader\": \"default_rule\"\n"
                + "        },\n"
                + "        \"port\": 4443\n"
                + "    },\n"
                + "    \"loggingConfig\": {\n"
                + "        \"level\": \"INFO\",\n"
                + "        \"handlers\": \"java.util.logging.FileHandler, java.util.logging.ConsoleHandler,de.urbanpulse.transfer.AccessLogFileHandler\",\n"
                + "        \"pattern\": \"%t/Outboundv%g.log\",\n"
                + "        \"access_log_level\": \"INFO\",\n"
                + "        \"access_log_pattern\": \"%t/ui-vertx-outbound-access-%g.log\",\n"
                + "        \"patternInfo\": \"%t is the system TEMP folder, %g is the generation number to distinguish between rotating logs.\",\n"
                + "        \"limit\": \"1024000\",\n"
                + "        \"count\": \"5\",\n"
                + "        \"formatter\": \"java.util.logging.SimpleFormatter\"\n"
                + "    },\n"
                + "    \"circuitBreakerOptions\": {\n"
                + "        \"maxFailures\": 5,\n"
                + "        \"timeout\": 10000,\n"
                + "        \"fallbackOnFailure\": false,\n"
                + "        \"resetTimeout\": 10000\n"
                + "    },\n"
                + "  \"eventBusImplementation\": {\n"
                + "    \"class\": \"de.urbanpulse.eventbus.vertx.VertxEventbusFactory\",\n"
                + "    \"address\": \"theOutbound\"\n"
                + "  }"
                + "}");

        upServiceConfig = new JsonObject()
          .put("jdbc", new JsonObject()
          );

        validConfig.put("upServiceConfig", upServiceConfig);
    }

    @After
    public void stop() {
        vertx.close();
    }

    @Test
    public void test_createCommandHandler_returnsAOutboundCommandHandler() throws NoSuchFieldException {
        CommandHandler handlerUnderTest = mainVerticle.createCommandHandler();

        assertTrue(handlerUnderTest instanceof OutboundCommandHandler);
    }

    @Test
    public void test_resetModule(TestContext context) {
        Async async = context.async();

        Handler<Void> callback = (Void result) -> {
            context.assertNull(result);
            async.complete();
        };

        OutboundCommandHandler commandHandlerMock = Mockito.mock(OutboundCommandHandler.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            CommandResult cllback = invocation.getArgument(0);
            cllback.done(null, null);
            return null;
        }).when(commandHandlerMock).reset(any());

        mainVerticle.setOutboundCommandHandler(commandHandlerMock);
        mainVerticle.resetModule(callback);

    }

    @Test
    public void test_createRegisterModuleConfig_returnsValidConfig() {
        Map<String, Object> objectUnderTest = mainVerticle.createRegisterModuleConfig();

        assertEquals("OutboundInterface", objectUnderTest.get("moduleType"));
    }

    @Test
    public void test_setupModule_was_successful(TestContext context) {
        Async async = context.async();
        OutboundCommandHandler commandHandlerMock = Mockito.mock(OutboundCommandHandler.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            CommandResult callback = invocation.getArgument(1);
            callback.done(null, null);
            return null;
        }).when(commandHandlerMock).setup(any(), any());

        Handler<AsyncResult<Void>> callback = (AsyncResult<Void> result) -> {
            context.assertTrue(result.succeeded());
            async.complete();
        };

        mainVerticle.setOutboundCommandHandler(commandHandlerMock);
        mainVerticle.setupModule(null, callback);

    }

    @Test
    public void test_setupModule_was_not_successful(TestContext context) {
        Async async = context.async();
        OutboundCommandHandler commandHandlerMock = Mockito.mock(OutboundCommandHandler.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            CommandResult callback = invocation.getArgument(1);
            callback.done(new JsonObject(), null);
            return null;
        }).when(commandHandlerMock).setup(any(), any());

        Handler<AsyncResult<Void>> callback = (AsyncResult<Void> result) -> {
            context.assertTrue(result.failed());
            context.assertEquals("Setup failed.", result.cause().getMessage());
            async.complete();
        };

        mainVerticle.setOutboundCommandHandler(commandHandlerMock);
        mainVerticle.setupModule(null, callback);
    }

    @Test
    public void test_deploy_fails_empty_config(TestContext context) {
        Async async = context.async();

        vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(new JsonObject()), (hndlr) -> {
            context.assertTrue(hndlr.failed());
            context.assertEquals("Config is empty (or corrupted) and thus not valid", hndlr.cause().getMessage());
            async.complete();
        });
    }

    @Test
    public void test_deploy_fails_no_security_config(TestContext context) {
        Async async = context.async();
        validConfig.getJsonObject("MainVerticle").putNull("security");
        vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(validConfig), (hndlr) -> {
            context.assertTrue(hndlr.failed());
            context.assertEquals("Security manager config missing!", hndlr.cause().getMessage());
            async.complete();
        });
    }

    @Test
    public void test_ws_server_not_deployed(TestContext context) {
        Async async = context.async();
        validConfig.put("wsServerConfig", new JsonObject());
        vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(validConfig), (hndlr) -> {
            context.assertTrue(hndlr.failed());
            context.assertEquals("WS server deployment failed!", hndlr.cause().getMessage());
            async.complete();
        });
    }

    @Test
    public void test_IncomingEventReceiver_not_deployed(TestContext context) {
        Async async = context.async();
        JsonObject config = new JsonObject("{\n"
                + "    \"MainVerticle\": {\n"
                + "        \"security\": {\n"
                + "            \"jdbc\": {\n"
                + "                \"driverClassName\": \"org.postgresql.Driver\",\n"
                + "                \"username\": \"urbanuser\",\n"
                + "                \"password\": \"dfgkdf93nv73n\",\n"
                + "                \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n"
                + "                \"maxActive\": 8,\n"
                + "                \"maxIdle\": 8,\n"
                + "                \"initialSize\": 1\n"
                + "            },\n"
                + "            \"keycloak\": {\n"
                + "              \"clientId\": \"urbanpulse\",\n"
                + "              \"secret\": \"<client-secert>\",\n"
                + "              \"apiBaseUrl\": \"http://localhost:9080/auth\",\n"
                + "              \"realm\": \"ui\"\n"
                + "            }\n"
                + "        }\n"
                + "    },\n"
                + "	\"sharedServerConfig\": {\n"
                + "        \"host\": \"0.0.0.0\",\n"
                + "        \"encrypt\": true,\n"
                + "        \"keystore\": \"localhost_keystore.jks\",\n"
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
                + "    \"wsServerConfig\": {\n"
                + "        \"externalHost\": \"localhost\",\n"
                + "        \"portSecure\": 3210,\n"
                + "        \"portInsecure\": 3211,\n"
                + "        \"basePathWithLeadingSlashOnly\": \"/OutboundInterfaces/outbound\",\n"
                + "        \"enableMetrics\": false,\n"
                + "        \"comment\": \"OutboundInterfaces like in UP1\"\n"
                + "    }\n"
                + "}");
        config.put("upServiceConfig", upServiceConfig);

        vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(config), (hndlr) -> {
            context.assertTrue(hndlr.failed());
            context.assertEquals("incoming event receiver deployment failed!", hndlr.cause().getMessage());
            async.complete();
        });

    }



    @Test
    public void test_All_Verticles_Deploy(TestContext context) {
        Async async = context.async();
        vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(validConfig), (hndlr) -> {
            context.assertTrue(hndlr.succeeded());
            context.assertEquals(7, vertx.deploymentIDs().size());
            async.complete();
        });
    }

}
