package de.urbanpulse.persistence.v3.inbound;

import de.urbanpulse.eventbus.EventbusFactory;
import de.urbanpulse.eventbus.MessageConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import de.urbanpulse.persistence.v3.storage.StorageService;
import static de.urbanpulse.persistence.v3.storage.cache.FirstLevelStorageConst.FIRST_LEVEL_STORAGE_SERVICE_ADDRESS;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * simple implementation for persisting into AzureTableStore
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class InboundVerticle extends AbstractVerticle {

    private final static Logger LOGGER = LoggerFactory.getLogger(InboundVerticle.class);

    private PrioritizingHashMap eventQueues;

    private StorageService firstLevelStorage;
    private MessageConsumer messageConsumer;
    private Counter totalEventsReceivedCounter;

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject secondLevelConfig = config().getJsonObject("secondLevelConfig", new JsonObject());
        JsonObject eventBusConfig = config().getJsonObject("eventBusImplementation");

        int maxBatchSize = secondLevelConfig.getInteger("maxBatchSize", 1000);
        long maxTimeMillis = secondLevelConfig.getLong("maxTimeMillis", 5000L);
        eventQueues = new PrioritizingHashMap(maxBatchSize, maxTimeMillis);


        firstLevelStorage = new ServiceProxyBuilder(vertx)
                .setAddress(FIRST_LEVEL_STORAGE_SERVICE_ADDRESS)
                .build(StorageService.class);

        String inputAddress = config().getString("inputAddress");
        String pullAddress = config().getString("pullAddress");

        EventbusFactory eventbusFactory = EventbusFactory.createFactory(vertx, eventBusConfig);
        LOGGER.info("starting " + this + " listening for events on " + inputAddress);

        if (pullAddress != null) {
            vertx.eventBus().localConsumer(pullAddress, this::sendEvents);
        }

        eventbusFactory.createMessageConsumer(eventBusConfig, hndlr -> {
            if (hndlr.succeeded()) {
                messageConsumer = hndlr.result();
                messageConsumer.handleEvent(message -> {
                    LOGGER.debug("+++ Received message in InboundVerticle: " + message.toString());
                    cacheAndEnqueueReceivedEvent(message.toJsonArray());
                });
                startPromise.complete();
            } else {
                LOGGER.error(hndlr.cause());
                startPromise.fail(hndlr.cause());
            }
        });

    }

    /**
     *
     * @param message a Void EventBus Message
     */
    private void sendEvents(Message<Void> message) {
        ArrayList<JsonObject> response = new ArrayList<>();
        eventQueues.drainMostImportant(response);
        message.reply(new JsonArray(response));
    }

    private void cacheAndEnqueueReceivedEvent(final JsonArray events) {
        incTotalEventsCounter(events.size());
        List<JsonObject> validEvents = StreamSupport.stream(events.spliterator(), false)
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(e -> e.containsKey("SID"))
                .filter(e -> e.containsKey("timestamp"))
                .filter(e -> e.getValue("SID") instanceof String)
                .collect(Collectors.toList());
        validEvents.forEach(this::enqueueEvent);

        firstLevelStorage.persist(validEvents);

    }



    private void incTotalEventsCounter(double amount) {
        if (totalEventsReceivedCounter != null) {
            totalEventsReceivedCounter.increment(amount);
        }
    }

    private void enqueueEvent(JsonObject event) {
        try {
            final String sid = event.getString("SID");
            eventQueues.putIfAbsent(sid, new LinkedBlockingQueue<>());
            LinkedBlockingQueue<JsonObject> eventQueue = eventQueues.get(sid);
            eventQueue.put(event);
        } catch (InterruptedException ex) {
            LOGGER.error("enqueue operation interrupted!", ex);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        messageConsumer.close();
        stopPromise.complete();
    }
}
