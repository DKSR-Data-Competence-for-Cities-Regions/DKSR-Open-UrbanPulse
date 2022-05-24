package de.urbanpulse.urbanpulsecontroller.admin;

import javax.ejb.ApplicationException;

/**
 * thrown when an entity is created that references another entity which was not found by its ID
 *
 * throwing this causes a rollback
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = true)
public class ReferencedEntityMissingException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReferencedEntityMissingException(String message) {
        super(message);
    }
}
