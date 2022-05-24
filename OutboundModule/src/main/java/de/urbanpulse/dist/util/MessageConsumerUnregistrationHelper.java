package de.urbanpulse.dist.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MessageConsumerUnregistrationHelper {

    public void unregister(Collection<MessageConsumer> consumers, Handler<AsyncResult<Void>> handler) {
        Deque<MessageConsumer> remaining = new LinkedList<>(consumers);
        unregisterRemaining(remaining, handler);
    }

    private void unregisterRemaining(Deque<MessageConsumer> remaining, Handler<AsyncResult<Void>> resultHandler) {
        if (remaining.isEmpty()) {
            resultHandler.handle(Future.succeededFuture(null));
            return;
        }
        MessageConsumer consumer = remaining.pop();
        consumer.unregister(event -> unregisterRemaining(remaining, resultHandler));
    }
}
