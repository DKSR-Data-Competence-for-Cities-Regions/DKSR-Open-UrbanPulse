package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import java.util.Collections;
import java.util.List;
import javax.ejb.ApplicationException;
import io.vertx.core.json.JsonObject;

/**
 * application exception with forced rollback (thrown in case of failures in
 * {@link EventProcessorWrapper})
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = true)
public class EventProcessorWrapperException extends Exception {

    protected List<JsonObject> errorList;

    public EventProcessorWrapperException() {
        super();
    }

    public EventProcessorWrapperException(String message) {
        super(message);
    }

    public EventProcessorWrapperException(String message, List<JsonObject> errorList) {
        super(message);
        this.errorList = errorList;
    }

    public EventProcessorWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public List<JsonObject> getErrorList() {
        return this.errorList != null ? errorList : Collections.emptyList();
    }
}
