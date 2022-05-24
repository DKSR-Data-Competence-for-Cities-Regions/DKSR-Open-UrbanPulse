package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.ConnectionHandler;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
public class AsyncConnectionHandler {

    private static final Logger LOGGER = Logger.getLogger(AsyncConnectionHandler.class.getName());

    @Asynchronous
    public Future<JsonObject> handleIncomingMessageAsync(ConnectionHandler connectionHandler, JsonObject message) {
        AtomicReference<JsonObject> json = new AtomicReference<>();
        connectionHandler.handleIncomingMessage(message, json::set);
        return new AsyncResult<>(json.get());
    }

    @Asynchronous
    public void replySent(ConnectionHandler connectionHandler, String receiverId, JsonObject msg, Throwable error) {
        try {
            connectionHandler.replySent(receiverId, msg, error);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception in connectionHandler.replySent", e);
        }
    }
}
