package de.urbanpulse.transfer;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * handles transaction commands, keeps an undo stack to be executed in case of a rollback
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransactionManager {

    private final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
    public static final String METHOD_BEGIN = "transactionBegin";
    public static final String METHOD_COMMIT = "transactionCommit";
    public static final String METHOD_ROLLBACK = "transactionRollback";

    List<UndoCommand> undoCommands = new ArrayList<>();
    boolean insideTransaction = false;

    private final ErrorChecker errorChecker = new ErrorChecker();
    private final ErrorFactory errorFactory = new ErrorFactory();

    public TransactionManager() {
    }

    /**
     * Handle transaction commands [{@literal begin, commit, rollback}]
     * @param method the transaction method to perform
     * @param callback function that is called after execution of the transaction commands
     * @return true if the method is a transaction-method
     */
    boolean handleCommand(String method, CommandResult callback) {
        switch (method) {
            case METHOD_BEGIN:
                begin(callback);
                return true;
            case METHOD_COMMIT:
                commit(callback);
                return true;
            case METHOD_ROLLBACK:
                rollback(callback);
                return true;
        }
        return false;
    }

    void begin(CommandResult callback) {
        if (insideTransaction) {
            logger.debug("receiving transaction begin while inside transaction");
        }
        undoCommands.clear();
        insideTransaction = true;
        callback.done(new JsonObject(), null);
    }

    void commit(CommandResult callback) {
        undoCommands.clear();
        insideTransaction = false;
        callback.done(new JsonObject(), null);
    }

    private void rollback(final List<UndoCommand> cmds, final List<JsonObject> results, final Handler<Void> callback) {
        if (cmds.isEmpty()) {
            callback.handle(null);
            return;
        }
        UndoCommand undoCommand = cmds.remove(0);
        undoCommand.execute(new CommandResult() {
            @Override
            public void done(JsonObject result, UndoCommand cmd) {
                results.add(result);
                if (!errorChecker.isError(result)) {
                    rollback(cmds, results, callback);
                }
            }
        });
    }

    void rollback(final CommandResult callback) {
        final List<JsonObject> results = new ArrayList<>();
        rollback(undoCommands, results, new Handler<Void>() {
            @Override
            public void handle(Void event) {
                JsonObject result = new JsonObject();
                for (JsonObject r : results) {
                    if (errorChecker.isError(r)) {
                        result = r;
                        break;
                    }
                }
                undoCommands.clear();
                insideTransaction = false;
                callback.done(result, null);
            }
        });
    }

    void addUndoCommand(UndoCommand cmd) {
        if (!insideTransaction) {
            logger.debug("adding undo command while NOT inside transaction");
        }
        undoCommands.add(0, cmd);  // LIFO
    }

    /**
     *
     * @return {@code true} if inside a transaction
     */
    boolean insideTransaction() {
        return insideTransaction;
    }
}
