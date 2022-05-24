package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import javax.ejb.ApplicationException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = true)
public class PersistenceV3WrapperException extends Exception {

     public PersistenceV3WrapperException() {
        super();
    }

    public PersistenceV3WrapperException(String message) {
        super(message);
    }

    public PersistenceV3WrapperException(String message, Throwable cause) {
        super(message, cause);
    }

}
