package de.urbanpulse.cep;

import de.urbanpulse.eventbus.EventbusFactory;
import de.urbanpulse.eventbus.MessageConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CEPEventReceiver extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(CEPEventReceiver.class);
    private MessageConsumer messageConsumer;
    private Counter totalEventsReceivedCounter;

    @Override
    public void start(Promise<Void> startPromise) {
        LOG.info("Starting CEPEventReceiver verticle");


        JsonObject eventBusConfig = config().getJsonObject("eventBusImplementation");
        EventbusFactory eventbusFactory = EventbusFactory.createFactory(vertx, eventBusConfig);

        eventbusFactory.createMessageConsumer(eventBusConfig, hndlr -> {
            if (hndlr.succeeded()) {
                messageConsumer = hndlr.result();
                startPromise.complete();
                messageConsumer.handleEvent(message -> {
                    LOG.debug("++++ Received message CEPEventReceiver: " + message.toString());
                    sendToEsper(message.toString());
                });
            } else {
                startPromise.fail(hndlr.cause());
            }
        });
    }

    private void registerMeters(MeterRegistry registry) {
        if (registry != null) {
            totalEventsReceivedCounter = Counter.builder("up_cep_events_received")
                    .description("Number of events received.")
                    .register(registry);
        }
    }

    private void incTotalEventsCounter(double amount) {
        if (totalEventsReceivedCounter != null) {
            totalEventsReceivedCounter.increment(amount);
        }
    }

    private void sendToEsper(String message) {
        JsonArray events = new JsonArray(message);
        incTotalEventsCounter(events.size());
        LOG.trace("Sending events to Esper");
        vertx.eventBus().send(MainVerticle.ESPER_IN, events);
    }

    @Override
    public void stop() throws Exception {
        messageConsumer.close();
        super.stop();
    }

}
