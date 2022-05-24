package de.urbanpulse.persistence.v3.inbound;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.urbanpulse.persistence.v3.storage.PredefinedEvents;
import de.urbanpulse.persistence.v3.storage.cache.NullFirstLevelStorage;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
public class InboundVerticleTest {

    private static final String PULL_ADDRESS = "thePersistencePullTest";

    private Vertx vertx = null;
    private JsonObject optionsJSON = new JsonObject();
    private InboundVerticle verticle;

    /**
     * Read the config and deploy the {@link InboundVerticle}
     *
     * @param context vertx's TestContext for async testing
     * @throws IOException some I/O went bad
     */
    @Before
    public void setUp(TestContext context) throws IOException {
        BackendRegistries.setupBackend(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true))
                .setEnabled(true));

        verticle = new InboundVerticle();
        vertx = Vertx.vertx();
        String fileName = "test-config.json";
        String configStr = new String(Files.readAllBytes(Paths.get(fileName)));
        optionsJSON = new JsonObject(configStr);
        final JsonObject levelXStorageConfig = new JsonObject().put("implementation", NullFirstLevelStorage.class.getName());
        optionsJSON.put("storageConfig", new JsonObject()
                .put("firstLevelConfig", levelXStorageConfig)
                .put("secondLevelConfig", levelXStorageConfig))
                .put("pullAddress", PULL_ADDRESS)
                .put("eventBusImplementation", new JsonObject()
                        .put("class", "de.urbanpulse.eventbus.vertx.VertxEventbusFactory")
                        .put("address", "thePersistence"));
        DeploymentOptions options = new DeploymentOptions().setConfig(optionsJSON);
        vertx.deployVerticle(InboundVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        vertx.close(completionHandler -> async.complete());
    }

    /**
     * all events should be forwarded to storage
     *
     * @param context vertx's TestContext for async testing
     */
    @Test
    public void testValidEvents(TestContext context) {
        Async async = context.async();
        sendTestEvents(PredefinedEvents.validObjects);
        Promise<Void> promise = Promise.promise();
        pullEvents(promise, 4);

        promise.future().onSuccess(handler -> {

            async.complete();
        });
    }

    /**
     * This will send the testObjects to the InboundVerticle.
     *
     * @param testObjects the objects that should be sent to the InboundVerticle
     */
    private void sendTestEvents(JsonObject[] testObjects) {
        String inputAddress = optionsJSON.getString("inputAddress");
        Stream.of(testObjects)
                .forEach(event -> vertx.eventBus().send(inputAddress, toArray(event).toBuffer()));
    }

    private void pullEvents(Promise<Void> promise, int expectedEvents) {
        vertx.setTimer(1000L, hndlrTimer -> {
            vertx.eventBus().send(PULL_ADDRESS, null, hndlr -> {
                System.out.println(hndlr.result().body());
                JsonArray response = (JsonArray) hndlr.result().body();
                if (response.size() == expectedEvents) {
                    promise.complete();
                } else {
                    promise.fail("Unexpected amount fo events received, expected: "
                            + expectedEvents + " got: " + response.size());
                }
            });
        });
    }

    /**
     * @param jsonObject a JsonObject
     * @return returns a JsonArray that contains only the given JsonObject or an empty JsonArray if
     * the JsonObject is null
     */
    private JsonArray toArray(JsonObject jsonObject) {
        final JsonArray jsonArray = new JsonArray();
        if (jsonObject != null) {
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    @Test
    public void testInvalidEvents(TestContext context) {
        sendTestEvents(ArrayUtils.addAll(PredefinedEvents.invalidObjects));
        Promise<Void> promise = Promise.promise();
        pullEvents(promise, 0);
        Async async = context.async();
        promise.future().onSuccess(handler -> async.complete())
                .onFailure(handler -> context.fail(handler.getCause()));
    }

    /**
     * only the valid events should be forwarded to storage
     *
     * @param context vertx's TestContext for async testing
     */
    @Test
    public void testAllEvents(TestContext context) {
        sendTestEvents(ArrayUtils.addAll(PredefinedEvents.validObjects, PredefinedEvents.invalidObjects));
        Promise<Void> promise = Promise.promise();
        pullEvents(promise, 4);
        Async async = context.async();
        promise.future().onSuccess(handler -> async.complete())
                .onFailure(handler -> context.fail(handler.getCause()));
    }

    private void undeploy() {
        vertx.deploymentIDs().forEach(id -> vertx.undeploy(id));
    }

    @Test
    public void test_deploys_and_undeploy_properly(TestContext context) {
        undeploy();
        Async async = context.async();
        DeploymentOptions options = new DeploymentOptions().setConfig(optionsJSON);

        vertx.deployVerticle(verticle, options, hndlr -> {
            if (hndlr.failed()) {
                context.fail();
            }
            vertx.undeploy(hndlr.result(), hndlr2 -> {
                if (hndlr2.succeeded()) {
                    async.complete();
                } else {
                    context.fail();
                }
            });
        });
    }
}
