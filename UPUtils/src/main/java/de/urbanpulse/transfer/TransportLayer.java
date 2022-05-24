package de.urbanpulse.transfer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface TransportLayer {

    void send(String receiverId, String msg);

    void send(String receiverId, JsonObject msg);

    void publish(final String address, JsonObject message);

    /**
     * @param address vert.x address on which we receive messages
     * @param connectionHandler the connectionHandler to be registered
     * @param callback resultHandler callback that allows to check for
     * success/failure
     */
    void registerConnectionHandler(final String address, final ConnectionHandler connectionHandler, final Handler<AsyncResult<Void>> callback);

    /**
     * @param address connection ID (i.e. vert.x address of the receiver)
     * @param message the message to be sent
     * @param callback handler for processing the result (which may be an error
     * object)
     * @param timeout in MSec
     */
    void sendWithTimeout(final String address, JsonObject message, long timeout, final Handler<JsonObject> callback);

    /**
     * unregister any handlers we registered the vert.x eventBus
     * <p>
     * required for clean shutdown in JEE!
     */
    void unregisterHandlers();

}
