package de.urbanpulse.urbanpulsecontroller.modules.vertx.master.transfer;

import de.urbanpulse.transfer.ConnectionHandler;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.TransferStructureFactory;
import de.urbanpulse.transfer.TransportLayer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.urbanpulse.transfer.ErrorFactory.ERROR_ORGINAL_MESSAGE;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.AsyncConnectionHandler;
import io.vertx.core.Vertx;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * central part of the transport layer that can be used both in a verticle as
 * well as a POJO/bean in JEE
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransportLayerJEE implements TransportLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportLayerJEE.class);

    private final Map<String, MessageConsumer<JsonObject>> handlers = new HashMap<>();
    private final EventBus eventBus;
    private final ErrorFactory errorFactory = new ErrorFactory();
    private final DeliveryOptions deliveryTimeout;
    private final boolean logEventBusContent;

    private final AsyncConnectionHandler asyncConnectionHandler;
    private final Vertx vertx;

    /**
     * @param vertx vertx instance
     * @param logEventBusContent if true, log content of sent and received
     * eventBus messages (useful for debugging, but in some cases it may leak
     * sensitive information such as credentials - NOT to be used on production
     * deployments!)
     * @param asyncMessageHandler used to handle the received messages in a JEE thread
     */
    public TransportLayerJEE(Vertx vertx, AsyncConnectionHandler asyncMessageHandler, boolean logEventBusContent) {
        this.eventBus = vertx.eventBus();
        this.vertx = vertx;
        this.asyncConnectionHandler = asyncMessageHandler;
        deliveryTimeout = new DeliveryOptions();
        deliveryTimeout.setSendTimeout(AbstractMainVerticle.SM_TIMEOUT);
        this.logEventBusContent = logEventBusContent;
    }

    /**
     * @param address vert.x address on which we receive messages
     * @param connectionHandler handles the communication between the modules
     * @param callback resultHandler callback that allows to check for
     * success/failure
     */
    @Override
    public void registerConnectionHandler(final String address, final ConnectionHandler connectionHandler,
            final Handler<AsyncResult<Void>> callback) {
        if (handlers.containsKey(address)) {
            LOGGER.error("Already registered handler for " + address + " exists");
            handlers.get(address).unregister(event -> registerHandler(connectionHandler, address, callback));
        } else {
            registerHandler(connectionHandler, address, callback);
        }
    }

    /**
     * unregister any handlers we registered the vert.x eventBus
     * <p>
     * required for clean shutdown in JEE!
     */
    @Override
    public void unregisterHandlers() {
        final Set<String> keys = new HashSet<>(handlers.keySet());
        keys.forEach(address -> {
            MessageConsumer<JsonObject> consumer = handlers.get(address);
            consumer.unregister();
            handlers.remove(address);
        });
    }

    @Override
    public void publish(final String receiverId, JsonObject message) {
        eventBus.publish(receiverId, message);
    }

    @Override
    public void send(String receiverId, JsonObject msg) {
        eventBus.send(receiverId, msg);
    }

    @Override
    public void send(String receiverId, String msg) {
        eventBus.send(receiverId, msg);
    }

    /**
     * @param receiverId connection ID (i.e. vert.x address of the receiver)
     * @param message a JsonObject representing the message
     * @param callback handler for processing the result (which may be an error
     * object)
     * @param timeout in milliseconds
     */
    @Override
    public void sendWithTimeout(final String receiverId, JsonObject message, long timeout, final Handler<JsonObject> callback) {
        if (logEventBusContent) {
            LOGGER.info("sending to " + receiverId + ": " + message + " with timeout: " + timeout);
        }

        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setSendTimeout(timeout);
        vertx.runOnContext(action
                -> eventBus.request(receiverId, message, deliveryOptions, (AsyncResult<Message<JsonObject>> result) -> {
                    JsonObject resultMessage;
                    if (result.succeeded()) {
                        resultMessage = result.result().body();
                        if (result.result().replyAddress() != null) {
                            LOGGER.debug("sending empty reply as acknowledge");
                            result.result().reply(null);
                        }
                    } else {
                        resultMessage = createErrorResult(result);
                        putIncomingMessageToResultMessage(resultMessage, message);
                    }
                    logReceivedMessage(receiverId, resultMessage);
                    callback.handle(resultMessage);
                })
        );
    }

    private JsonObject createErrorResult(AsyncResult<Message<JsonObject>> result) {
        JsonObject resultMessage;
        ReplyException ex = (ReplyException) result.cause();
        switch (ex.failureType()) {
            case NO_HANDLERS:
                resultMessage = errorFactory.createReplyNoHandler();
                break;
            case TIMEOUT:
                resultMessage = errorFactory.createReplyTimeout();
                break;
            case RECIPIENT_FAILURE:
                resultMessage = errorFactory.createReplyRecipientFailure();
                break;
            default:
                LOGGER.fatal("unknown reply failure type " + ex.failureType());
                resultMessage = errorFactory.createReplyRecipientFailure();
                break;
        }
        return resultMessage;
    }

    private void putIncomingMessageToResultMessage(JsonObject replyMessage, JsonObject incomingMessage) {
        LOGGER.debug("Putting original message to reply message");
        if (replyMessage.getJsonObject(TAG_BODY) == null) {
            JsonObject body = new JsonObject();
            body.put(ERROR_ORGINAL_MESSAGE, incomingMessage);
            replyMessage.put(TAG_BODY, body);
        } else {
            replyMessage.getJsonObject(TAG_BODY).put(ERROR_ORGINAL_MESSAGE, incomingMessage);
        }
        LOGGER.debug("Reply message with incoming message: " + replyMessage);
    }

    private void handleIncomingMessage(ConnectionHandler connectionHandler, Message<JsonObject> message) {
        vertx.<JsonObject>executeBlocking(res -> {
            try {
                Future<JsonObject> fu = asyncConnectionHandler.handleIncomingMessageAsync(connectionHandler, message.body());
                JsonObject result = fu.get(10, TimeUnit.SECONDS);
                res.complete(result);
            } catch (Exception ex) {
                LOGGER.error("Exception in TransportLayerJEE", ex);
                res.fail(ex);
            }
        }, futResult -> {
            if (futResult.succeeded()) {
                JsonObject result = futResult.result();
                final String receiverId = TransferStructureFactory.getSender(message.body());
                logReplyMessage(receiverId, result);
                message.replyAndRequest(result, deliveryTimeout, (AsyncResult<Message<JsonObject>> event) -> {

                    if (receiverId != null) {
                        asyncConnectionHandler.replySent(connectionHandler, receiverId, result, event.cause());
                    }
                });
            } else {
                LOGGER.fatal("Handling async result failed!", futResult.cause());
            }
        });
    }

    private void registerHandler(final ConnectionHandler connectionHandler, String address,
            final Handler<AsyncResult<Void>> callback) {
        MessageConsumer<JsonObject> consumer = eventBus.consumer(address, message -> {
            logReceivedMessage(address, message.body());
            handleIncomingMessage(connectionHandler, message);
        });
        if (callback != null) {
            consumer.completionHandler(callback);
        }
        handlers.put(address, consumer);
    }

    private void logReplyMessage(String receiverId, JsonObject message) {
        if (logEventBusContent) {
            LOGGER.info("reply to " + receiverId + ": " + message);
        }
    }

    private void logReceivedMessage(String receiverId, JsonObject resultMessage) {
        if (logEventBusContent) {
            LOGGER.info("received from " + receiverId + ": " + resultMessage);
        } else {
            LOGGER.info("received from " + receiverId + " (content not logged)");
        }
    }
}
