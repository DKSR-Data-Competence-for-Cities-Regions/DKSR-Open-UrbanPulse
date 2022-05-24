package de.urbanpulse.dist.inbound.http;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

import static de.urbanpulse.dist.inbound.http.HttpInboundCommandHandler.CONNECTOR_AUTH_MAP_NAME;
import static de.urbanpulse.dist.inbound.http.HttpInboundCommandHandler.SENSOR_EVENT_TYPES_MAP_NAME;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Promise;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class HttpReceiverVerticleTest {

    private static final String SEND_DESTINATION = "test.inbound.target";
    private static final String SID_1 = "sid1";
    private static final String EVENT_TYPE_1 = "eventType1";
    private static final String CONNECTOR_ID_1 = "1";
    private static final String CONNECTOR_KEY_1 = "key1";

    private Vertx vertx;
    private int receiverPort = 0;

    @Before
    public void setUp() {
        BackendRegistries.setupBackend(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true))
                .setEnabled(true));

        vertx = Vertx.vertx();
        vertx.sharedData().getLocalMap(SENSOR_EVENT_TYPES_MAP_NAME).put(SID_1, EVENT_TYPE_1);
        vertx.sharedData().getLocalMap(CONNECTOR_AUTH_MAP_NAME).put(CONNECTOR_ID_1, CONNECTOR_KEY_1);

        try {
            ServerSocket s = new ServerSocket(0);
            receiverPort = s.getLocalPort();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        vertx.close(completionHandler -> async.complete());
    }

    @Test
    public void testMetricsRegistration(TestContext context) {
        Async async = context.async();
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(false).setTrustAll(true));
        JsonObject testMsg = getTestMessage();

        HttpReceiverVerticle v = new HttpReceiverVerticle();
        Promise<String> result = Promise.promise();
        vertx.deployVerticle(v, new DeploymentOptions().setConfig(createVerticelConfig()), result);

        result.future().onSuccess(handler -> {
            webClient.post(receiverPort, "localhost", "/").sendJsonObject(testMsg, postHndlr -> {
                context.assertTrue(postHndlr.succeeded());
                context.assertEquals(204, postHndlr.result().statusCode());


                async.complete();
            });
        }).onFailure(handler -> context.fail(handler.getCause()));
    }

    private JsonObject getTestMessage() {
        JsonArray dataArray = new JsonArray().add(new JsonObject().put("test", "meh").put("SID", SID_1));
        return new JsonObject().put("data", dataArray);
    }

    @Test
    public void full_flow_successful(TestContext context) {
        Async async = context.async(3);
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(false).setTrustAll(true));

        JsonObject testMsg = getTestMessage();

        vertx.deployVerticle(HttpReceiverVerticle.class.getName(), new DeploymentOptions().setConfig(createVerticelConfig()), hndlr -> {
            context.assertTrue(hndlr.succeeded());
            async.countDown();
            webClient.post(receiverPort, "localhost", "/").sendJsonObject(testMsg, postHndlr -> {
                context.assertTrue(postHndlr.succeeded());
                context.assertEquals(204, postHndlr.result().statusCode());
                async.countDown();
            });
        });

        vertx.eventBus().localConsumer(SEND_DESTINATION, hndlr -> {
            Buffer buffer = (Buffer) hndlr.body();
            JsonArray msgAray = buffer.toJsonArray();

            context.assertEquals("meh", msgAray.getJsonObject(0).getString("test"));
            context.assertEquals(SID_1, msgAray.getJsonObject(0).getString("SID"));
            context.assertEquals(EVENT_TYPE_1, msgAray.getJsonObject(0).getJsonObject("_headers").getString("eventType"));

            async.countDown();
        });
    }

    @Test
    public void wrong_methodused_for_request(TestContext context) {
        Async async = context.async();
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(false).setTrustAll(true));
        JsonArray dataArray = new JsonArray().add(new JsonObject().put("test", "meh").put("SID", SID_1));
        JsonObject testMsg = new JsonObject().put("data", dataArray);

        vertx.deployVerticle(HttpReceiverVerticle.class.getName(), new DeploymentOptions().setConfig(createVerticelConfig()), hndlr -> {
            context.assertTrue(hndlr.succeeded());
            webClient.get(receiverPort, "localhost", "/").sendJsonObject(testMsg, postHndlr -> {
                context.assertTrue(postHndlr.succeeded());
                context.assertEquals(405, postHndlr.result().statusCode());
                async.complete();
            });
        });
    }

    private JsonObject createVerticelConfig() {
        return new JsonObject().put("sendDestinations", new JsonArray().add(SEND_DESTINATION))
                .put("host", "localhost")
                .put("port", receiverPort)
                .put("queueWorkerCount", 2)
                .put("queueBatchSize", 100000)
                .put("logMessages", true)
                .put("enforceHmac", false)
                .put("enforceTimestamp", false)
                .put("encrypt", false);
    }

}
