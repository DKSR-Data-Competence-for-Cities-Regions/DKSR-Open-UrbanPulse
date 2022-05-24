package de.urbanpulse.transfer;

import io.vertx.core.logging.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UndoCommand extends Command {

    Logger logger;
    Object commandHandler;
    private final ErrorFactory errorFactory = new ErrorFactory();

    public UndoCommand(Logger logger, Object commandHandler, String methodName, Map<String, Object> args) {
        super(methodName, args);
        this.logger = logger;
        this.commandHandler = commandHandler;
    }

    public void execute(CommandResult callback) {
        try {
            boolean createUndoCommand = false;
            java.lang.reflect.Method method = commandHandler.getClass().getMethod(methodName, Map.class, boolean.class, CommandResult.class);
            method.invoke(commandHandler, args, createUndoCommand, callback);
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                InvocationTargetException te = (InvocationTargetException) ex;
                logger.error(te.getTargetException());
            } else {
                logger.error(ex);
            }
            callback.done(errorFactory.createErrorMessage(ErrorFactory.COMMAND_NOT_EXECUTED), null);
        }
    }
}
