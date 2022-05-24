package de.urbanpulse.urbanpulsecontroller.admin;

import javax.ejb.ApplicationException;

/**
 * <p>thrown when an entity which should be added as a child of a node in the tree is already a parent of this
 * node or any of its parents, as this would create a cyclic dependency in the tree.</p>
 * throwing this causes a rollback
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = true)
public class CyclicDependencyException extends Exception {

    private static final long serialVersionUID = 1L;

    public CyclicDependencyException(String message) {
        super(message);
    }
}
