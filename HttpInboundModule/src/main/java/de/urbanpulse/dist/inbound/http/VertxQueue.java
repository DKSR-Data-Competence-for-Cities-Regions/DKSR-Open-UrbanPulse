package de.urbanpulse.dist.inbound.http;

import de.urbanpulse.eventbus.EventbusFactory;
import de.urbanpulse.eventbus.MessageProducer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VertxQueue extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxQueue.class);
    private LinkedBlockingQueue<JsonObject> queue;
    private JsonArray sendDestinations;
    private MessageConsumer<JsonArray> queueConsumer;
    private long lastFlush = 0l;
    private MessageProducer producer;

    @Override
    public void start(Promise<Void> startFuture) throws Exception {
        sendDestinations = config().getJsonArray("sendDestinations");
        JsonObject eventBusConfig = config().getJsonObject("eventBusImplementation", new JsonObject());

        queue = new LinkedBlockingQueue<>(10000);

        vertx.setTimer(5, this::checkQueue);

        queueConsumer = vertx.eventBus().<JsonArray>localConsumer("queue", message -> {
            List<JsonObject> list = message.body().stream().map(JsonObject.class::cast).collect(Collectors.toList());
            queue.addAll(list);
        });
        EventbusFactory eventbusFactory = EventbusFactory.createFactory(vertx, eventBusConfig);

        Promise<MessageProducer> producerPromise = Promise.promise();
        eventbusFactory.createMessageProducer(producerPromise);
        producerPromise.future().onSuccess(prod -> {
            producer = prod;
            startFuture.complete();
        }).onFailure(startFuture::fail);

    }

    private void sendMessages(JsonArray events) {
        if (events.size() > 0) {
            sendDestinations.stream().map(String.class::cast).forEach(sendDestination
                    -> producer.send(sendDestination, events.toBuffer(), sent -> {
                        if (sent.failed()) {
                            LOGGER.error("Failed to send message", sent.cause());
                        }
                    }));
        }
    }

    private void registerMeters(MeterRegistry registry) {
        if (registry != null) {
            Gauge.builder("inbound_events_in_queue", queue, LinkedBlockingQueue::size)
                    .description("Number of events in Queue.")
                    .register(registry);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {

        queueConsumer.unregister(result
                -> vertx.setPeriodic(10, id -> {
                    if (queue.isEmpty()) {
                        producer.close();
                        stopPromise.complete();
                    }
                })
        );

    }

    private void checkQueue(Long event) {
        if (queue.size() > 100 || (System.currentTimeMillis() - lastFlush) > 50) {
            List<JsonObject> events = new ArrayList<>();
            queue.drainTo(events);
            sendMessages(new JsonArray(events));
            lastFlush = System.currentTimeMillis();
        }

        vertx.setTimer(5, this::checkQueue);
    }
}
