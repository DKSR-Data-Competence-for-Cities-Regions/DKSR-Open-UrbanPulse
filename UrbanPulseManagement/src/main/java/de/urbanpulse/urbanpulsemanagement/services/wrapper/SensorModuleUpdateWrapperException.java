package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import io.vertx.core.json.JsonObject;
import java.util.List;
import javax.ejb.ApplicationException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = false)
public class SensorModuleUpdateWrapperException extends Exception {

    protected List<JsonObject> errorList;

    public SensorModuleUpdateWrapperException() {
        super();
    }

    public SensorModuleUpdateWrapperException(String message) {
        super(message);
    }

    public SensorModuleUpdateWrapperException(String message, List<JsonObject> errorList) {
        super(message, new RuntimeException(errorList.toString()));
        this.errorList = errorList;
    }

    public SensorModuleUpdateWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public List<JsonObject> getErrorList() {
        return this.errorList;
    }
}
