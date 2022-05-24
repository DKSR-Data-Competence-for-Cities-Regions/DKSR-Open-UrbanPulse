package de.urbanpulse.dist.outbound.server.historicaldata;

import de.urbanpulse.outbound.PersistenceQueryService;
import de.urbanpulse.outbound.PersistenceQueryServiceVertxEBProxy;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.Base64;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
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
public class HistoricalDataRestVerticleSecureTest {

    private Vertx vertx;

    @Mock
    private PersistenceQueryServiceVertxEBProxy persistenceQueryProxy;

    @Mock
    private DefaultSecurityManager secureSecurityManager;

    @Mock
    private Subject mockedSubject;

    // The verticle under test
    @InjectMocks
    private HistoricalDataRestVerticle testicle;

    @Before
    public void setUp(TestContext context) throws IOException {
        MockitoAnnotations.initMocks(this);
        when(secureSecurityManager.createSubject(any())).thenReturn(mockedSubject);
        SecurityUtils.setSecurityManager(secureSecurityManager);

        vertx = Vertx.vertx();
        Mockito.doAnswer((Answer<Void>) (InvocationOnMock invocation) -> {
            String uniqueRequestHandle = invocation.getArgument(4, String.class);
            Handler<AsyncResult<Void>> handler = invocation.getArgument(5, Handler.class);
            handler.handle(Future.succeededFuture());

            JsonObject response = new JsonObject()
                    .put("batch", new JsonArray())
                    .put("isLast", true)
                    .put("batchTimestamp", LocalDateTime.now().toString());
            vertx.eventBus().send(uniqueRequestHandle, response);
            return null;
        }).
                when(persistenceQueryProxy).query(any(), anyString(), any());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testUnauthorizedResultsIn401(TestContext context) throws IOException {
        int port = getFreePort();
        Async async = context.async();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject()))
                .put("roles_to_rules", new JsonObject().put("admin", "default_rule"));

        doThrow(new AuthenticationException()).when(mockedSubject).login(any());

        vertx.deployVerticle(testicle, new DeploymentOptions().setConfig(config), whenDone -> {
            HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port).setSsl(false);
            HttpClient client = vertx.createHttpClient(options);
            HttpClientRequest request = client.get("/UrbanPulseData/historic/sensordata?since=2017-07-02T00:00:00.000Z&until=2017-07-04T00:00:00.000Z&sid=105", response -> {
                context.assertEquals(401, response.statusCode());
                response.bodyHandler(bodyHandler -> {
                    String body = bodyHandler.toString();
                    context.assertTrue(body.contains("Invalid credentials."));
                    verify(mockedSubject, times(1)).login(any());
                    async.complete();
                });
            });
            request.putHeader("Authorization", authorization("horst", "fummel"));
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
        JsonObject defaultFilters = new JsonObject();
        JsonObject config = new JsonObject()
                .put("persistenceQueryAddress", "testPQA")
                .put("port", port)
                .put("encrypt", false)
                .put("rules", new JsonObject().put("default_rule", new JsonObject().put("defaultFilters", defaultFilters)))
                .put("roles_to_rules", new JsonObject().put("admin", "default_rule"));

        doNothing().when(mockedSubject).login(any());
        when(mockedSubject.getPrincipal()).thenReturn("The User name");
        when(mockedSubject.hasRoles(any()))
                .thenReturn(new boolean[]{true});
        when(mockedSubject.isPermitted(anyString())).thenReturn(true);

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
