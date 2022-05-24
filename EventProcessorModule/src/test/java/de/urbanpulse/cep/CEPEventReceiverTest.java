package de.urbanpulse.cep;

import de.urbanpulse.eventbus.vertx.VertxEventbusFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;
import java.util.Collection;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class CEPEventReceiverTest {

    private Vertx vertx;
    private CEPEventReceiver cepEventReceiver;

    @Before
    public void setUp(TestContext context) {
        VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(
                new MicrometerMetricsOptions()
                        .setPrometheusOptions(new VertxPrometheusOptions()
                                .setEnabled(true))

                        .setEnabled(true));

        vertx = Vertx.vertx(vertxOptions);


        JsonObject eventBusImplementation = new JsonObject()
                .put("class", VertxEventbusFactory.class.getName())
                .put("address", "TestIn");
        JsonObject config = new JsonObject()
                .put("eventBusImplementation", eventBusImplementation);
        cepEventReceiver = new CEPEventReceiver();

        vertx.deployVerticle(cepEventReceiver, new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
    }




}
