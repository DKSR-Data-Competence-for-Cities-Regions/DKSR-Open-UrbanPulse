package de.urbanpulse.eventbus.vertx;

import de.urbanpulse.eventbus.MessageConsumer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VertxMessageConsumer implements MessageConsumer {

    private io.vertx.core.eventbus.MessageConsumer<Buffer> consumer;
    private final Vertx vertx;
    private final JsonObject config;

    public VertxMessageConsumer(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public void handleEvent(Handler<Buffer> messageHandler) {
        if (consumer != null) {
            consumer.unregister();
        }
        consumer = vertx.eventBus().<Buffer>consumer(config.getString("address"), message -> {
            messageHandler.handle(message.body());
            message.reply("");
        });
    }

    @Override
    public void close() {
        consumer.unregister();
    }

}
