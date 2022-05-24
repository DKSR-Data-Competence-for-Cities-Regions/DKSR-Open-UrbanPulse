package de.urbanpulse.transfer;

import static de.urbanpulse.transfer.ErrorFactory.ERROR_ORGINAL_MESSAGE;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import de.urbanpulse.util.status.UPModuleState;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConnectionHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);

    private Object commandHandler;
    private String connectionId;

    private TransportLayer transportLayer;
    private TransactionManager transactionManager;

    private final TransferStructureFactory transferStructureFactory;
    private final ErrorChecker errorChecker = new ErrorChecker();
    private final ErrorFactory errorFactory = new ErrorFactory();

    public ConnectionHandler() {
        this.transferStructureFactory = new TransferStructureFactory();
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setCommandHandler(Object commandHandler) {
        this.commandHandler = commandHandler;
    }

    public void setTransport(TransportLayer transportLayer) {
        this.transportLayer = transportLayer;
    }

    public void setConnectionId(String connectionId, final Handler<AsyncResult<Void>> callback) {
        this.connectionId = connectionId;
        transportLayer.registerConnectionHandler(connectionId, this, callback);
    }

    public void sendCommand(final String receiverId, String command, Map<String, Object> args, long timeout,
            final Handler<JsonObject> callback) {
        JsonObject msg = transferStructureFactory.createTransferStructure(connectionId, receiverId, command, args);
        LOGGER.debug("Sending command with " + timeout + " timeout: " + msg + " ms");
        transportLayer.sendWithTimeout(receiverId, msg, timeout, callback::handle);
    }

    public void sendExitProcess(final String receiverId, long timeout, final Handler<JsonObject> callback) {
        JsonObject msg = transferStructureFactory.createExitProcess(connectionId);
        sendSpecialCommand(receiverId, msg, timeout, callback);
    }

    public void sendResetConnection(final String receiverId, long timeout, final Handler<JsonObject> callback) {
        JsonObject msg = transferStructureFactory.createResetConnection(connectionId);
        sendSpecialCommand(receiverId, msg, timeout, callback);
    }

    public void sendHeartBeat(final String receiverId, long timeout, UPModuleState state, final Handler<JsonObject> callback) {
        JsonObject msg = transferStructureFactory.createHeartbeat(connectionId, state);
        sendSpecialCommand(receiverId, msg, timeout, callback);
    }

    private void sendSpecialCommand(final String receiverId, JsonObject msg, long timeout, final Handler<JsonObject> callback) {
        transportLayer.sendWithTimeout(receiverId, msg, timeout, (JsonObject result) -> {
            if (errorChecker.isConnectionError(result)) {
                LOGGER.error("Error while sending special command " + result);
            }
            callback.handle(result);
        });
    }

    private void handleCommandResult(JsonObject replyMessage, UndoCommand cmd, JsonObject incomingMessage, Handler<JsonObject> callback) {
        if (cmd != null) {
            transactionManager.addUndoCommand(cmd);
        }
        LOGGER.debug("Checking error in replyMessage: " + replyMessage);
        if (errorChecker.isError(replyMessage)) {
            LOGGER.warn("Error while handling command");
            putIncomingMessageToResultMessage(replyMessage, incomingMessage);
        }
        JsonObject msg = replyMessage;
        String senderId = TransferStructureFactory.getSender(incomingMessage);
        if (senderId != null) {
            if (errorChecker.isError(replyMessage)) {
                msg = transferStructureFactory.putTransferHeadersIntoMessage(connectionId, senderId, replyMessage);
            } else {
                msg = transferStructureFactory.createTransferStructure(connectionId, senderId, replyMessage);
            }
        }
        LOGGER.debug("Reply message is: " + msg);
        callback.handle(msg);
    }

    public void handleIncomingMessage(JsonObject incomingMessage, final Handler<JsonObject> callback) {
        LOGGER.debug("Handling incoming message: " + incomingMessage);
        try {
            JsonObject messageBody = filterSpecialCommands(incomingMessage);
            final boolean isSpecialCommand = messageBody != null;
            if (!isSpecialCommand) {
                messageBody = incomingMessage.getJsonObject(TAG_BODY);
                if (messageBody == null) {
                    JsonObject result = errorFactory.createBodyError();
                    LOGGER.warn("Body of incoming message is null");
                    putIncomingMessageToResultMessage(result, incomingMessage);
                    callback.handle(result);
                    return;
                }
            } else {
                LOGGER.info("Received special command " + messageBody.getString(TransferStructureFactory.TAG_BODY_METHOD));
            }
            LOGGER.debug("Handling command...");
            handleCommand(messageBody,
                    (JsonObject replyMessage, UndoCommand cmd)
                    -> handleCommandResult(replyMessage, cmd, incomingMessage, callback)
            );
        } catch (Exception ex) {
            LOGGER.error("Exception occurred while handling incoming message", ex);
            JsonObject result = errorFactory.createBodyError();
            putIncomingMessageToResultMessage(result, incomingMessage);
            callback.handle(result);
        }
    }

    private void putIncomingMessageToResultMessage(JsonObject replyMessage, JsonObject incomingMessage) {
        if (replyMessage.getJsonObject(TAG_BODY) == null) {
            JsonObject body = new JsonObject();
            body.put(ERROR_ORGINAL_MESSAGE, incomingMessage);
            replyMessage.put(TAG_BODY, body);
        } else {
            replyMessage.getJsonObject(TAG_BODY).put(ERROR_ORGINAL_MESSAGE, incomingMessage);
        }
        LOGGER.debug("Reply message with incoming message:" + replyMessage);
    }

    public void replySent(String receiverId, JsonObject msg, Throwable error) {
        if (error != null) {
            LOGGER.error("can't send reply to {0} : {1}", receiverId, error.getMessage());
            return;
        }
        final JsonObject header = msg.getJsonObject(TransferStructureFactory.TAG_HEADER);
        if (header == null) {
            LOGGER.error("replySent to {0}: header is null!", receiverId);
        } else if (header.containsKey(ErrorFactory.ERROR_CODE_TAG)) {
            LOGGER.error("replySent to {0}: {1}", receiverId, msg.encode());
        }
    }

    private void handleCommand(JsonObject command, CommandResult callback) {
        String method = command.getString(TransferStructureFactory.TAG_BODY_METHOD);
        if (method == null) {
            callback.done(errorFactory.createBodyError(), null);
            return;
        }

        if (transactionManager.handleCommand(method, callback)) {
            return;
        }

        boolean createUndoCommand = transactionManager.insideTransaction();
        callCommand(method, command.getJsonObject(TransferStructureFactory.TAG_BODY_ARGS).getMap(), createUndoCommand, callback);
    }

    /**
     * Creates and starts a thread that invokes the method {@code methodName} on
     * the {@code commandHandler}
     *
     * @param methodName name of the method to be invoked
     * @param argsMap a map containing the arguments
     * @param createUndoCommand if command should be created or undone
     * @param callback the callback object
     */
    private void callCommand(String methodName, Map<String, Object> argsMap, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("Invoke method [" + methodName + "] via reflection");
        try {
            java.lang.reflect.Method method = commandHandler.getClass().getMethod(methodName, Map.class, boolean.class,
                    CommandResult.class);
            method.invoke(commandHandler, argsMap, createUndoCommand, callback);
        } catch (Exception ex) {  // catch everything
            LOGGER.error("callCommand failed ", ex);
            JsonObject errorMessage = errorFactory.createErrorMessage(ErrorFactory.COMMAND_NOT_EXECUTED);
            LOGGER.debug("Error message for calling commnd: " + errorMessage);
            callback.done(errorMessage, null);
        }
    }

    public void reset() {
        connectionId = null;
        transportLayer.unregisterHandlers();
    }

    /**
     * @param incomingMessage any JSON-object
     * @return a TransferStructure for heartbeat, resetConnection, exitProcess
     * or null if the {@code incomingMessage} doesn't contain of those keys
     */
    private JsonObject filterSpecialCommands(JsonObject incomingMessage) {
        JsonObject heartBeat = incomingMessage.getJsonObject(TransferStructureFactory.COMMAND_HEARTBEAT);
        if (heartBeat != null) {
            return transferStructureFactory.createHeartbeatCommand(heartBeat);
        }
        String resetConnection = incomingMessage.getString(TransferStructureFactory.COMMAND_RESET_CONNECTION);
        if (resetConnection != null) {
            return transferStructureFactory.createResetConnectionCommand(resetConnection);
        }
        String exitProcess = incomingMessage.getString(TransferStructureFactory.COMMAND_EXIT_PROCESS);
        if (exitProcess != null) {
            return transferStructureFactory.createExitProcessCommand(exitProcess);
        }
        return null;
    }
}
