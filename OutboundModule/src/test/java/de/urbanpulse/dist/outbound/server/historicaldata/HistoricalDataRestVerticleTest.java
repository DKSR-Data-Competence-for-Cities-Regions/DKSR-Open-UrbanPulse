package de.urbanpulse.dist.outbound.server.historicaldata;

import de.urbanpulse.auth.upsecurityrealm.shiro.UnsecurityManager;
import de.urbanpulse.outbound.PersistenceQueryService;
import de.urbanpulse.outbound.PersistenceQueryServiceVertxEBProxy;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ServiceBinder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Base64;

import org.apache.shiro.SecurityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class HistoricalDataRestVerticleTest {

    private Vertx vertx;

    @Mock
    private PersistenceQueryServiceVertxEBProxy persistenceQueryProxy;


    // The verticle under test
    @InjectMocks
    private HistoricalDataRestVerticle testicle;

    @Before
    public void setUp(TestContext context) throws IOException {
        SecurityUtils.setSecurityManager(new UnsecurityManager());
        MockitoAnnotations.initMocks(this);
        vertx = Vertx.vertx();
        Mockito.doAnswer((Answer<Void>) (InvocationOnMock invocation) -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(2, Handler.class);
            handler.handle(Future.succeededFuture());
            return null;
        }).
                when(persistenceQueryProxy).query(any(), anyString(), any());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testPersistenceNotAvailableReturns500(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));
        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T12:00:00.000Z&until=2017-07-04T12:00:00.000Z&sid=105", response -> {
                context.assertEquals(500, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains(ResponseDefFactory.ERROR_PERSISTENCE_NOT_AVAILABLE));
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testSinceMissingReturns400(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?until=2017-07-04T12:00:00.000Z&sid=105", response -> {
                context.assertEquals(400, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains(ResponseDefFactory.ERROR_BAD_REQUEST));
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testUntilMissingReturnsIn400(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-04T12:00:00.000Z&sid=105", response -> {
                context.assertEquals(400, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains(ResponseDefFactory.ERROR_BAD_REQUEST));
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testSinceInWrongFormatReturns400(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T:00:00.000Z&until=2017-07-04T12:00:00.000Z&sid=105", response -> {
                context.assertEquals(400, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains(ResponseDefFactory.ERROR_BAD_REQUEST));
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testUntilInWrongFormatReturns400(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T00:00:00.000Z&until=2017-07-04T:00:00.000Z&sid=105", response -> {
                context.assertEquals(400, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains(ResponseDefFactory.ERROR_BAD_REQUEST));
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testSuccessfulRequestReturns200(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField = HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T00:00:00.000Z&until=2017-07-04T00:00:00.000Z&sid=105", response -> {
                context.assertEquals(200, response.statusCode());
                async.complete();
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testSIDMissingReturns400(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T00:00:00.000Z&until=2017-07-04T00:00:00.000Z", response -> {
                context.assertEquals(400, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(!body.isEmpty());
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });

    }

    private String authorization(String username, String password) {
        if (username == null && password == null) {
            return null;
        } else if (username == null) {
            throw new IllegalArgumentException("Unable to create authorization - missing username");
        } else if (password == null) {
            throw new IllegalArgumentException("Unable to create authorization - missing password");
        }
        String base64key;
        try {
            base64key = Base64.getEncoder()
                    .encodeToString(new StringBuilder(username).append(":").append(password).toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException(ex);
        }
        return "Basic " + base64key;
    }

    @Test
    public void testBackPressure_bug8189(TestContext context) throws IOException {
        int port = getFreePort();
        String persistenceQueryAddress = "testPQA";

        ServiceBinder serviceBinder = new ServiceBinder(vertx);
        MessageConsumer<JsonObject> serviceMock = serviceBinder.setAddress(persistenceQueryAddress).register(PersistenceQueryService.class, new PersistenceQueryServiceImplMockWithBackPressure(vertx));

        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(HistoricalDataRestVerticle.class.getName(), new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2018-04-01T00:00:00.000Z&until=2018-04-02T12:00:00.000Z&sid=105", response -> {
                context.assertEquals(200, response.statusCode());
                response.handler(resp -> {
                    if (resp.toString().contains("]}")) {
                        serviceBinder.unregister(serviceMock);
                        async.complete();
                    }
                });
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });

    }


    @Test
    public void testFailedEventTypeRequestReturns500(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField = HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?eventType=TestEventType", response -> {
                context.assertEquals(500, response.statusCode());
                async.complete();
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testSuccessfulEventTypeRequestReturns200(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));


        vertx.eventBus().localConsumer("vertx://UrbanPulseService", hndlr -> {
            String action = hndlr.headers().get("action");
            switch (action) {
                case "getSIDsForEventTypeId":
                case "getSIDsForEventTypeName":
                    hndlr.reply(new JsonArray().add("1"));
                    break;
                case "eventTypeIdExists":
                case "eventTypeNameExists":
                    hndlr.reply(Boolean.TRUE);
                    break;
                case "getEventTypeNameForEventTypeId":
                    hndlr.reply("TestEventType");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        });

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField = HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?eventType=TestEventType", response -> {
                context.assertEquals(200, response.statusCode());
                async.complete();
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testUnknownEventTypeIdReturns404(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));


        vertx.eventBus().localConsumer("vertx://UrbanPulseService", hndlr -> {
            String action = hndlr.headers().get("action");
            switch (action) {
                case "getSIDsForEventTypeId":
                case "getSIDsForEventTypeName":
                    hndlr.reply(new JsonArray());
                    break;
                case "eventTypeIdExists":
                case "eventTypeNameExists":
                    hndlr.reply(Boolean.FALSE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        });

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField = HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?eventType=TestEventType", response -> {
                context.assertEquals(404, response.statusCode());
                async.complete();
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testUnknownEventTypeNameReturns404(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule").put("admin", "default_rule"));


        vertx.eventBus().localConsumer("vertx://UrbanPulseService", hndlr -> {
            String action = hndlr.headers().get("action");
            switch (action) {
                case "getSIDsForEventTypeId":
                case "getSIDsForEventTypeName":
                    hndlr.reply(new JsonArray());
                    break;
                case "eventTypeIdExists":
                case "eventTypeNameExists":
                    hndlr.reply(Boolean.FALSE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        });

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField = HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?eventTypeName=TestEventType", response -> {
                context.assertEquals(404, response.statusCode());
                async.complete();
            });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.end();
        });
    }

    @Test
    public void testHandlerCheckCorsHeadersWillAddHeadersIfOriginsPresent(TestContext context)
            throws IOException {
        int port = getFreePort();
        Async async = context.async();

        JsonObject config = new JsonObject().put("persistenceQueryAddress", "testPQA")
                .put("port", port).put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("historic_data_reader", "default_rule")
                        .put("admin", "default_rule"));

        vertx.eventBus().localConsumer("vertx://UrbanPulseService", hndlr -> {
            String action = hndlr.headers().get("action");
            switch (action) {
                case "getSIDsForEventTypeId":
                case "getSIDsForEventTypeName":
                    hndlr.reply(new JsonArray().add("1"));
                    break;
                case "eventTypeIdExists":
                case "eventTypeNameExists":
                    hndlr.reply(Boolean.FALSE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        });

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            try {
                Field persistenceQueryProxyField =
                        HistoricalDataRestVerticle.class.getDeclaredField("persistenceQueryProxy");
                persistenceQueryProxyField.setAccessible(true);
                persistenceQueryProxyField.set(testicle, persistenceQueryProxy);
            } catch (Exception ex) {
                context.fail(ex);
            }

            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost")
                    .setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request =
                    client.get("/UrbanPulseData/historic/sensordata", response -> {
                        context.assertEquals("*",
                                response.headers().get("Access-Control-Allow-Origin"));
                        async.complete();
                    });
            request.putHeader("Authorization", authorization("horst", "hummel"));
            request.putHeader("origin", "urbanpulse.de");
            request.putHeader("host", "localhost");
            request.end();
        });
    }

    /**
     * private method to
     *
     * @return
     */
    private int getFreePort() throws IOException {
        int freePort;
        try (ServerSocket socketConnection = new ServerSocket(0)) {
            freePort = socketConnection.getLocalPort();
            socketConnection.close();
            return freePort;
        }

    }
}
