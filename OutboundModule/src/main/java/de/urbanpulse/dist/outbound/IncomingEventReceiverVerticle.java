package de.urbanpulse.dist.outbound;

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
 * handles receiving incoming events (using its own event loop, separate from the other verticles)
 * <p>
 * Deploy only one instance per OutboundModule!
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class IncomingEventReceiverVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingEventReceiverVerticle.class);
    private MessageConsumer messageConsumer;
    private Counter totalEventsReceivedCounter;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("registering for events...");

        JsonObject eventBusConfig = config().getJsonObject("eventBusImplementation");
        EventbusFactory eventbusFactory = EventbusFactory.createFactory(vertx, eventBusConfig);

        eventbusFactory.createMessageConsumer(eventBusConfig, hndlr -> {
            if (hndlr.succeeded()) {
                messageConsumer = hndlr.result();
                startPromise.complete();
                messageConsumer.handleEvent(message -> {
                    LOGGER.debug("++++ Received message IncomingEventReceiverVerticle: " + message.toString());
                    handleIncomingEvent(message.toString());
                });
            } else {
                LOGGER.error(hndlr.cause());
                startPromise.fail(hndlr.cause());
            }
        });
    }

    private void registerMeters(MeterRegistry registry) {
        if (registry != null) {
            totalEventsReceivedCounter = Counter.builder("up_outbound_events_received")
                    .description("Number of events received.")
                    .register(registry);
        }
    }

    private void incTotalEventsCounter(double amount) {
        if (totalEventsReceivedCounter != null) {
            totalEventsReceivedCounter.increment(amount);
        }
    }

    private void handleIncomingEvent(String message) {
        JsonObject msg = new JsonObject(message);
        final JsonArray eventsArray = msg.getJsonArray("data");

        incTotalEventsCounter(eventsArray.size());

        for (Object obj : eventsArray) {
            JsonObject event = (JsonObject) obj;
            String statementName = event.getString("statementName");
            publishToLocalListenersForStatement(statementName, event);
        }
    }

    private void publishToLocalListenersForStatement(String statementName, JsonObject event) {
        vertx.eventBus().publish(MainVerticle.LOCAL_STATEMENT_PREFIX + statementName, event);
    }

    @Override
    public void stop() throws Exception {
        messageConsumer.close();
        super.stop();
    }
}
