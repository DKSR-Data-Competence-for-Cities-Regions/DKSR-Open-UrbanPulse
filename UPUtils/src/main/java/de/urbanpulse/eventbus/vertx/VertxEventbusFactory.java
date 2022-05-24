package de.urbanpulse.eventbus.vertx;

import de.urbanpulse.eventbus.EventbusFactory;
import de.urbanpulse.eventbus.MessageConsumer;
import de.urbanpulse.eventbus.MessageProducer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VertxEventbusFactory implements EventbusFactory {
    private static final Logger LOG = LoggerFactory.getLogger(VertxEventbusFactory.class);
    private final Vertx vertx;

    public VertxEventbusFactory(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        LOG.debug("Creating Vert'x EventBus Client with config: ",jsonObject.encode());
    }

    @Override
    public void createMessageConsumer(JsonObject config, Handler<AsyncResult<MessageConsumer>> handler) {
        if (config.containsKey("address")) {
            handler.handle(Future.succeededFuture(new VertxMessageConsumer(vertx, config)));
        } else {
            handler.handle(Future.failedFuture("Address is missing from the config for the VertxMessageConsumer!"));
        }
    }

    @Override
    public void createMessageProducer(Handler<AsyncResult<MessageProducer>> handler) {
        handler.handle(Future.succeededFuture(new VertxMessageProducer(vertx)));
    }

}
