package de.urbanpulse.urbanpulsecontroller.modules.vertx.master.exceptions;

import javax.enterprise.inject.spi.DeploymentException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VertxClusterException extends DeploymentException {

    public VertxClusterException(String message, InterruptedException ex) {
        super(message, ex);
    }

    public VertxClusterException(String message) {
        super(message);
    }

}
