package de.urbanpulse.urbanpulsemanagement.util;

import javax.ejb.ApplicationException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = false)
public class EventTypeRegistrarException extends RuntimeException {

    public EventTypeRegistrarException(String message) {
        super(message);
    }

}
