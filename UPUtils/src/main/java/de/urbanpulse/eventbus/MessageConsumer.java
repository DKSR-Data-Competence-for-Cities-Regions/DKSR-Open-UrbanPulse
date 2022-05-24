package de.urbanpulse.eventbus;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface MessageConsumer {
    void handleEvent(Handler<Buffer> messageHandler);
    void close();
}
