package de.urbanpulse.persistence.v3;

import java.util.Map;

import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * {@link CommandHandler} for PersistenceV3
 */
public class PersistenceCommandHandler extends CommandHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(PersistenceCommandHandler.class);

    public PersistenceCommandHandler(AbstractMainVerticle mainVerticle) {
        super(mainVerticle);
    }

    @Override
    public void exitProcess(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("exitProcess command received");
        super.exitProcess(args, createUndoCommand, callback);
    }

    @Override
    public void resetConnection(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("resetConnection command received");
        super.resetConnection(args, createUndoCommand, callback);
    }

    @Override
    public void resetModule(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("resetModule command received");
        super.resetModule(args, createUndoCommand, callback);
    }

}
