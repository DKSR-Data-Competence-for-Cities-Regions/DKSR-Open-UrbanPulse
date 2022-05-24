package de.urbanpulse.urbanpulsecontroller.modules.vertx;

/**
 * possible transaction states of a vert.x module
 * ({@link #STARTED}, {@link #SENT}, {@link #SUCCESSFUL}, {@link #FAILED}
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public enum UPTransactionState {
    /**
     * transaction started
     */
    STARTED,
    /**
     * command sent to the respective module
     */
    SENT,
    /**
     * command execution successful
     */
    SUCCESSFUL,
    /**
     * command execution failed
     */
    FAILED
}
