package de.urbanpulse.transfer;

import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CommandHandler {

    protected final AbstractMainVerticle mainVerticle;

    public CommandHandler(AbstractMainVerticle mainVerticle) {
        this.mainVerticle = mainVerticle;
    }

    public void exitProcess(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        callback.done(new JsonObject(), null);
        mainVerticle.exitProcess(args);
    }

    public void resetConnection(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        callback.done(new JsonObject(), null);
        mainVerticle.resetConnection();
    }

    public void resetModule(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        callback.done(new JsonObject(), null);
        mainVerticle.resetModule();
    }

}
