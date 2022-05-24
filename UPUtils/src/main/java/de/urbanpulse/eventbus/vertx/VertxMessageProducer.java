package de.urbanpulse.eventbus.vertx;

import de.urbanpulse.eventbus.MessageProducer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VertxMessageProducer implements MessageProducer {
    private Vertx vertx;

    public VertxMessageProducer(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void send(String target, Buffer message, Handler<AsyncResult<Void>> resultHandler) {
        vertx.eventBus().send(target, message);
    }

    @Override
    public void close() {
        //we have nothing to close in here
    }
}
