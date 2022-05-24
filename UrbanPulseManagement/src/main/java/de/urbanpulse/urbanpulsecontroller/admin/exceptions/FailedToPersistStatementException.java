package de.urbanpulse.urbanpulsecontroller.admin.exceptions;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class FailedToPersistStatementException extends Exception {

    public FailedToPersistStatementException() {
    }

    public FailedToPersistStatementException(String message) {
        super(message);
    }

    public FailedToPersistStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToPersistStatementException(Throwable cause) {
        super(cause);
    }

    public FailedToPersistStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }


}
