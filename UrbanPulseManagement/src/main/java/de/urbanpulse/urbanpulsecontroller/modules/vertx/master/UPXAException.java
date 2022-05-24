package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import io.vertx.core.json.JsonObject;
import javax.ejb.ApplicationException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException
class UPXAException extends Exception {

    private final String error;

    UPXAException(InterruptedException ex) {
        super(ex);
        error = null;
    }

    UPXAException() {
        error = null;
    }

    UPXAException(String message) {
        super(message);
        error = null;
    }

    UPXAException(JsonObject error) {
        super(error.toString());
        this.error = error.encode();
    }

    public String getErrorJson() {
        if (error == null) {
            return null;
        }

        return error;
    }

}
