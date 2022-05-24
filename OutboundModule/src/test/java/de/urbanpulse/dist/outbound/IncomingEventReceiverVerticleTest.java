package de.urbanpulse.dist.outbound;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class IncomingEventReceiverVerticleTest {

    private static final String ADDRESS = "outboundDestinationAddress";
    private static final String STATEMENT_NAME = "TestStatement";

    Vertx vertx;
    JsonObject validConfig;
    IncomingEventReceiverVerticle incomingEventReceiverVerticle;

    @Before
    public void setUp() {
        VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(
                new MicrometerMetricsOptions()
                        .setPrometheusOptions(new VertxPrometheusOptions()
                                .setEnabled(true))
                        .setEnabled(true));

        vertx = Vertx.vertx(vertxOptions);

        incomingEventReceiverVerticle = new IncomingEventReceiverVerticle();

        validConfig = new JsonObject("{\n"
                + "\"eventBusImplementation\": {\n"
                + "    \"class\": \"de.urbanpulse.eventbus.vertx.VertxEventbusFactory\",\n"
                + "    \"address\": \"" + ADDRESS + "\"\n"
                + "   }"
                + "}");
    }

    @After
    public void stop(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void test_Events_get_handled_and_published(TestContext context) {
        Async async = context.async(2);

        JsonObject dummyMessage = new JsonObject("{\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"statementName\": \"statement1\",\n"
                + "      \"payload\": \"dummy1\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"statementName\": \"statement2\",\n"
                + "      \"payload\": \"dummy2\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");

        vertx.eventBus().localConsumer(MainVerticle.LOCAL_STATEMENT_PREFIX + "statement1", hndlr -> {
            JsonObject event = (JsonObject) hndlr.body();
            if (event.getString("payload").equals("dummy1")) {
                async.countDown();
            } else {
                context.fail();
            }
        });

        vertx.eventBus().localConsumer(MainVerticle.LOCAL_STATEMENT_PREFIX + "statement2", hndlr -> {
            JsonObject event = (JsonObject) hndlr.body();
            if (event.getString("payload").equals("dummy2")) {
                async.countDown();
            } else {
                context.fail();
            }
        });

        vertx.deployVerticle(incomingEventReceiverVerticle, new DeploymentOptions().setConfig(validConfig), hndlr -> {
            if (hndlr.failed()) {
                context.fail();
            }
            vertx.eventBus().send(ADDRESS, dummyMessage.toBuffer());
        }
        );
    }



    @Test
    public void test_handleIncomingEvent_willIncreaseTotalEventsCounter(TestContext context) {
        Async async = context.async();

        vertx.deployVerticle(incomingEventReceiverVerticle, new DeploymentOptions().setConfig(validConfig), hndlr -> {
            JsonObject event = new JsonObject().put("statementName", STATEMENT_NAME).put("payload", "Something");
            JsonArray array = new JsonArray().add(event.copy()).add(event.copy());
            JsonObject wrapped = new JsonObject().put("data", array);
            vertx.eventBus().send(ADDRESS, wrapped.toBuffer());
        });
    }

}
