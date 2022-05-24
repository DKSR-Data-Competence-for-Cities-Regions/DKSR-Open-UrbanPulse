package de.urbanpulse.cep;

import city.ui.shared.commons.time.UPDateTimeFormat;
import static de.urbanpulse.cep.MainVerticle.ESPER_IN;
import de.urbanpulse.transfer.CommandHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private MainVerticle mainVerticle;

    Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        BackendRegistries.setupBackend(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true))
                .setEnabled(true));

        Async async = context.async();
        vertx = Vertx.vertx();
        mainVerticle = new MainVerticle();
        DeploymentOptions options = new DeploymentOptions().setConfig(getConfig());
        vertx.deployVerticle(mainVerticle, options, hndlr -> {
            assertTrue(hndlr.succeeded());
            async.complete();
        });
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        vertx.close(completionHandler -> async.complete());
    }



    @Test
    public void test_generateEventMap_with_null__headers_parameter(TestContext context) {
        Map<String, Object> eventConfig = new HashMap<>();
        eventConfig.put("_headers", "java.util.Map");
        eventConfig.put("timestamp", "java.util.Date");
        eventConfig.put("SID", "string");

        JsonObject event = new JsonObject();
        event.putNull("_headers");
        event.put("SID", "theSid");

        String timestamp = UPDateTimeFormat.getFormatterWithZoneZ().format(ZonedDateTime.now(ZoneId.of("UTC")));

        event.put("timestamp", timestamp);
        Map<String, Object> result = mainVerticle.generateEventMap(event, eventConfig);
        assertNull(result.get("_headers"));
    }

    @Test
    public void test_generateEventMap_willGracefullySkipField_ifDateParameterHasWrongFormat(TestContext context) {
        Map<String, Object> eventConfig = new HashMap<>();
        eventConfig.put("wrongFormatField", "java.util.Date");
        eventConfig.put("rightFormatField", "java.util.Date");

        JsonObject event = new JsonObject();
        event.put("wrongFormatField", "2019-08-05T10:10:00Z"); // Missing milliseconds
        event.put("rightFormatField", "2019-08-05T10:10:00.000Z");

        Map<String, Object> result = mainVerticle.generateEventMap(event, eventConfig);
        assertEquals(1, result.size());
        assertFalse(result.containsKey("wrongFormatField"));
        assertTrue(result.containsKey("rightFormatField"));
        assertTrue(result.get("rightFormatField") instanceof java.util.Date);
    }

    @Test
    public void test_createCommandHandler(TestContext context) {
        CommandHandler commandHandler = mainVerticle.createCommandHandler();
        assertNotNull(commandHandler);
    }

    private JsonObject getConfig() {
        return new JsonObject("{\n"
                + "    \"startDelay\": 10,\n"
                + "    \"cepReceiverVerticleInstances\": 1,\n"
                + "    \"logOutEvents\": false,\n"
                + "    \"logLatency\": false,\n"
                + "    \"cepDestination\": \"theCEP_test\",\n"
                + "    \"logRateForSIDErrors\": 0,\n"
                + "    \"logEventBusContent\": false,\n"
                + "    \"enableJsonParsing\": true,\n"
                + "    \"enableMapCreation\": true,\n"
                + "    \"enableEsper\": false,\n"
                + "    \"loggingConfig\": {\n"
                + "        \"level\": \"INFO\",\n"
                + "        \"handlers\": \"java.util.logging.FileHandler, java.util.logging.ConsoleHandler\",\n"
                + "        \"pattern\": \"%t/CEP%g.log\",\n"
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
                + "       \"class\": \"de.urbanpulse.eventbus.vertx.VertxEventbusFactory\",\n"
                + "       \"url\": \"pulsar://localhost:6650\",\n"
                + "       \"subscriptionName\": \"cep-consumer\",\n"
                + "       \"address\": \"theCEP\"\n"
                + "   }"
                + "}");

    }

}
