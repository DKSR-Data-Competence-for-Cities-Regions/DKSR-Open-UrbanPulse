package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.Command;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.CommandResultVerifier;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.DefaultCommandResultVerifier;
import java.util.Map;

/**
 * wraps a command to be sent to a certain module instance via a {@link ConnectionHandlerJEE} within a transaction
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransactionalCommandTask {

    private final Command command;
    private final CommandResultVerifier resultVerifier;
    private final String moduleId;

    public static final long DEFAULT_TIMEOUT = 3000;

    private static final CommandResultVerifier DEFAULT_VERIFIER = new DefaultCommandResultVerifier();
    private final long timeout;
    private ConnectionHandlerJEE connectionHandler;

    public TransactionalCommandTask(String moduleId, Command command, ConnectionHandlerJEE connectionHandler) {
        this(moduleId, command, DEFAULT_VERIFIER, connectionHandler, DEFAULT_TIMEOUT);
    }

    public TransactionalCommandTask(String moduleId, Command command, CommandResultVerifier resultVerifier,
            ConnectionHandlerJEE connectionHandler, long timeout) {
        if (null == moduleId) {
            throw new IllegalArgumentException("moduleId null!");
        }
        if (null == command) {
            throw new IllegalArgumentException("command null!");
        }
        if (null == resultVerifier) {
            throw new IllegalArgumentException("resultVerifier null!");
        }
        if (null == connectionHandler) {
            throw new IllegalArgumentException("connectionHandler null!");
        }

        this.command = command;
        this.resultVerifier = resultVerifier;
        this.moduleId = moduleId;
        this.timeout = timeout;
        this.connectionHandler = connectionHandler;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getMethodName() {
        return command.getMethodName();
    }

    public Map<String, Object> getArgs() {
        return command.getArgs();
    }

    public Command getCommand() {
        return command;
    }

    public CommandResultVerifier getResultVerifier() {
        return resultVerifier;
    }

    public long getTimeout() {
        return timeout;
    }

    public ConnectionHandlerJEE getConnectionHandler() {
        return connectionHandler;
    }

}
