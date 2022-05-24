package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import io.vertx.core.json.JsonObject;

/**
 * wraps the result of {@link TransactionalCommandTask}s execution
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransactionalTaskResult {

    private final boolean successful;
    private final JsonObject result;

    public TransactionalTaskResult(boolean successful, JsonObject result) {
        this.successful = successful;
        this.result = result;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public JsonObject getResult() {
        return result;
    }
}
