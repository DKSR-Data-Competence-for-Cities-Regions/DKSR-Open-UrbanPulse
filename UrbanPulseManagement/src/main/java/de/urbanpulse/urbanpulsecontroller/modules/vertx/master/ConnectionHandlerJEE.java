package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.ErrorChecker;
import de.urbanpulse.transfer.TransactionManager;
import de.urbanpulse.transfer.TransferStructureFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import de.urbanpulse.transfer.TransportLayer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * wraps the connection to a vert.x module, allows to send custom commands as
 * well as transaction commands (start/rollback/commit)
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConnectionHandlerJEE implements UPXAResource {

    private static final long DEFAULT_TIMEOUT = 5000;

    private final Logger logger;
    private final String senderId;
    private final String receiverId;

    private final TransportLayer transportLayer;
    private String currentTransactionId = null;
    private final TransferStructureFactory transferStructureFactory;
    private final ErrorChecker errorChecker = new ErrorChecker();

    public ConnectionHandlerJEE(String receiverId, TransportLayer transportLayer) {
        this(LoggerFactory.getLogger(ConnectionHandlerJEE.class), receiverId, transportLayer);
    }

    public ConnectionHandlerJEE(Logger logger, String receiverId, TransportLayer transportLayer) {
        if (transportLayer == null) {
            throw new IllegalArgumentException("null transportLayer!");
        }

        if (logger == null) {
            throw new IllegalArgumentException("null logger!");
        }

        this.logger = logger;
        this.senderId = "sm_address";
        this.receiverId = receiverId;
        this.transportLayer = transportLayer;
        this.transferStructureFactory = new TransferStructureFactory();
    }

    private JsonObject sendCommand(String command) throws UPXAException {
        return sendCommand(command, new HashMap<>(), DEFAULT_TIMEOUT);
    }

    /**
     * send a command to the module
     *
     * @param command the command to be sent
     * @param args the arguments for the command
     * @param timeout represents the timeout for the sending of the command
     * @return command result object in case of success
     * @throws UPXAException in case of error
     */
    public JsonObject sendCommand(String command, Map<String, Object> args, long timeout) throws UPXAException {
        final AtomicReference<JsonObject> commandResult = new AtomicReference<>();
        final AtomicReference<JsonObject> connectionError = new AtomicReference<>();
        final CountDownLatch doneSignal = new CountDownLatch(1);
        JsonObject msg = transferStructureFactory.createTransferStructure(senderId, receiverId, command, args);
        transportLayer.sendWithTimeout(receiverId, msg, timeout, (JsonObject result) -> {
            if (!errorChecker.isConnectionError(result)) {
                commandResult.set(result);
            } else {
                connectionError.set(result);
            }
            doneSignal.countDown();
        });

        try {
            boolean success = doneSignal.await(timeout, TimeUnit.MILLISECONDS);
            if (!success) {
                java.util.logging.Logger.getLogger(ConnectionHandlerJEE.class.getName()).log(Level.SEVERE, "Timed out waiting for reply");
                throw new UPXAException("Timed out waiting for reply");
            }
            JsonObject error = connectionError.get();
            if (error != null) {
                throw new UPXAException(error);
            }
            return commandResult.get();
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(ConnectionHandlerJEE.class.getName()).log(Level.SEVERE, "wait for reply interrupted");
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            throw new UPXAException(ex);
        }
    }

    /**
     * inform the module that the specified transaction has started
     *
     * @param transactionId the transaction that has started
     * @throws UPXAException error executing command
     */
    @Override
    public void start(String transactionId) throws UPXAException {
        if (currentTransactionId != null) {
            final String message = "starting transaction " + transactionId + " while transaction " + currentTransactionId + " still running";
            logger.error(message);
            throw new UPXAException(message);
        }
        currentTransactionId = transactionId;
        sendCommand(TransactionManager.METHOD_BEGIN);
    }

    /**
     * inform the module that the specified transaction was successful and no
     * undo is necessary
     *
     * @param transactionId the transaction that will be commited
     * @throws UPXAException error executing command
     */
    @Override
    public void commit(String transactionId) throws UPXAException {
        if (!transactionId.equals(currentTransactionId)) {
            final String message = "using transaction " + transactionId + " while transaction " + currentTransactionId + " still running";
            logger.error(message);
            throw new UPXAException(message);
        }
        sendCommand(TransactionManager.METHOD_COMMIT);
        currentTransactionId = null;
    }

    /**
     * inform the module that the specified transaction failed and undo commands
     * for the successful parts need to be invoked
     *
     * @param transactionId he transaction that will be rolled back
     * @throws UPXAException error executing command
     */
    @Override
    public void rollback(String transactionId) throws UPXAException {
        if (currentTransactionId == null) {
            // If there never was a transaction start, no rollback has to be sent
            return;
        }

        if (!transactionId.equals(currentTransactionId)) {
            final String message = "using transaction " + transactionId + " while transaction " + currentTransactionId + " still running";
            logger.error(message);
            throw new UPXAException(message);
        }

        try {
            sendCommand(TransactionManager.METHOD_ROLLBACK);
        } finally {
            currentTransactionId = null;
        }
    }

}
