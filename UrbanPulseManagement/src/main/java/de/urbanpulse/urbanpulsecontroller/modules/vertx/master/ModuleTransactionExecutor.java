package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.transfer.ErrorChecker;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.TransactionEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.TransactionDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPTransactionState;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.CommandResultVerifier;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * executes commands for vert.x modules that require to be wrapped in a custom
 * transaction because they may affect multiple instances of one
 * {@link UPModuleType} and/or multiple {@link UPModuleType}s
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ModuleTransactionExecutor {

    @Inject
    private TransactionDAO transactionDAO;

    /**
     * @param tasks tasks that need to be run in a specific order, all will run
     * on the same thread
     * @param noHandlerModuleIDs the list of modules that have no handler
     * @return a list of errors encountered during execution of the tasks or
     * null if that specific task was successful (NOTE: ignores errors during
     * commit!)
     */
    public List<JsonObject> executeOrderDependentTasks(List<TransactionalCommandTask> tasks, List<String> noHandlerModuleIDs) {
        final String txId = UUID.randomUUID().toString();

        List<TransactionalTaskResult> results = execute(tasks, txId);
        boolean successful = allSuccessful(results);
        commitOrRollback(successful, txId, tasks);

        ErrorChecker errorChecker = new ErrorChecker();

        return results.stream().sequential().map(result -> {
            if ((result != null) && (!result.isSuccessful())) {
                JsonObject errorObj = result.getResult();
                ErrorFactory.ErrorCode errorCode = errorChecker.getConnectionErrorCode(errorObj);

                if (errorCode == ErrorFactory.ErrorCode.REPLY_NO_HANDLERS) {
                    noHandlerModuleIDs.add(errorObj.getString("failedModuleID"));
                }

                return errorObj;
            }

            return null;
        }).collect(Collectors.toList());
    }

    private void commitOrRollback(boolean successful, final String txId, List<TransactionalCommandTask> tasks) {
        if (successful) {
            commitAll(txId, tasks);
        } else {
            rollbackAll(txId, tasks);
        }

        transactionDAO.deleteByTxId(txId);
    }

    private List<TransactionalTaskResult> execute(List<TransactionalCommandTask> tasks, final String txId) {
        if (tasks.isEmpty()) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, "no tasks to execute in tx {0}", txId);
            return Collections.emptyList();
        }

        Logger.getLogger(getClass().getName()).log(Level.INFO, "executing {0} tasks for tx {1}", new Object[]{tasks.size(), txId});

        return tasks.stream().map(task -> executeTask(task, txId)).collect(Collectors.toList());
    }

    private TransactionalTaskResult executeTask(TransactionalCommandTask task, String txId) {
        String moduleId = task.getModuleId();
        Map<String, Object> args = task.getArgs();
        String method = task.getMethodName();
        long timeout = task.getTimeout();
        ConnectionHandlerJEE connectionHandler = task.getConnectionHandler();
        CommandResultVerifier resultVerifier = task.getResultVerifier();

        TransactionEntity txEntity = transactionDAO.create(txId, moduleId);

        JsonObject result = null;
        boolean successful = false;
        try {
            Logger.getLogger(getClass().getName()).log(Level.INFO, "starting task {0} on tx {1}", new Object[]{task, txId});

            connectionHandler.start(txId);
            transactionDAO.setTxState(txEntity, UPTransactionState.STARTED);

            Logger.getLogger(getClass().getName()).log(Level.INFO, "sending task {0} on tx {1}", new Object[]{task, txId});

            result = connectionHandler.sendCommand(method, args, timeout);
            transactionDAO.setTxState(txEntity, UPTransactionState.SENT);

            Logger.getLogger(getClass().getName()).log(Level.INFO, "verifying result of task {0} on tx {1}", new Object[]{task, txId});

            if (resultVerifier.verify(result)) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0}ok result of task  on tx {1}", new Object[]{task, txId});
                transactionDAO.setTxState(txEntity, UPTransactionState.SUCCESSFUL);
                successful = true;
            } else {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "bad result of task {0} on tx {1}", new Object[]{task, txId});
                transactionDAO.setTxState(txEntity, UPTransactionState.FAILED);
            }
        } catch (UPXAException ex) {
            String msg = "exception in task " + task + " on tx " + txId;
            Logger.getLogger(ModuleTransactionExecutor.class.getName()).log(Level.SEVERE,
                    msg, ex);
            transactionDAO.setTxState(txEntity, UPTransactionState.FAILED);

            if (ex.getMessage() != null) {
                JsonObject errorJson = new JsonObject(ex.getMessage());
                errorJson.put("failedModuleID", task.getModuleId());
                return new TransactionalTaskResult(false, errorJson);
            }
        }

        return new TransactionalTaskResult(successful, result);
    }

    private boolean allSuccessful(List<TransactionalTaskResult> results) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "checking {0} results", results.size());

        // we use reduce to make sure that every result is being fetched
        return results
                .stream()
                .sequential()
                .reduce(true, (intermediate, future) -> intermediate && future.isSuccessful(), Boolean::logicalAnd);
    }

    private void commitAll(String txId, List<TransactionalCommandTask> tasks) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "committing {0} tasks in tx {1}", new Object[]{tasks.size(), txId});

        AtomicInteger count = new AtomicInteger(0);
        tasks.forEach(task -> {
            try {
                task.getConnectionHandler().commit(txId);
                count.incrementAndGet();
            } catch (UPXAException ex) {
                String msg = "failed to commit task " + task;
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, msg, ex);
            }
        });

        Logger.getLogger(getClass().getName()).log(Level.INFO, "committed {0} / {1} tasks in tx {2}", new Object[]{count, tasks.size(), txId});
    }

    private void rollbackAll(String txId, List<TransactionalCommandTask> tasks) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "rolling back {0} tasks in tx {1}", new Object[]{tasks.size(), txId});

        AtomicInteger count = new AtomicInteger(0);
        tasks.forEach(task -> {
            try {
                task.getConnectionHandler().rollback(txId);
                count.incrementAndGet();
            } catch (UPXAException ex) {
                String msg = "failed to roll back task " + task;
                Logger.getLogger(getClass().getName()).log(Level.WARNING, msg, ex);
            }
        });

        Logger.getLogger(getClass().getName()).log(Level.INFO, "rolled back {0} / {1} tasks in tx {2}", new Object[]{count, tasks.size(), txId});
    }

}
