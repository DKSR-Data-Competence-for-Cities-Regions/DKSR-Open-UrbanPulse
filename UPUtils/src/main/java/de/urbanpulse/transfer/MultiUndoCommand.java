package de.urbanpulse.transfer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MultiUndoCommand extends UndoCommand {

    private final Deque<UndoCommand> undoCommands = new LinkedList<>();

    public MultiUndoCommand(Logger logger, Object commandHandler, String methodName, Map<String, Object> args) {
        super(logger, commandHandler, methodName, args);
    }

    public void add(UndoCommand undoCommand) {
        undoCommands.push(undoCommand);
    }

    @Override
    public void execute(CommandResult callback) {
        AtomicReference<JsonObject> overallResult = new AtomicReference<>(new JsonObject());
        while (!undoCommands.isEmpty()) {
            UndoCommand undoCommand = undoCommands.pop();
            undoCommand.execute((JsonObject result, UndoCommand cmd) -> {
                if (!isSuccessful(result)) {
                    overallResult.set(result);
                }
            });
            if (!isSuccessful(overallResult)) {
                break;
            }
        }

        callback.done(overallResult.get(), null);
    }

    private boolean isSuccessful(AtomicReference<JsonObject> result) {
        return isSuccessful(result.get());
    }

    private boolean isSuccessful(JsonObject result) {
        return result.isEmpty();
    }

}
