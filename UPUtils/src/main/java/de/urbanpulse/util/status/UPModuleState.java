package de.urbanpulse.util.status;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public enum UPModuleState {
    /**
     * HEALTHY means that the modules internal state is as desired.
     */
    HEALTHY,
    /**
     * UNSTABLE means that the module is in a state where it works at least partially, however it assumes it's state is not correct.
     */
    UNSTABLE,
    /**
     * UNHEALTHY means that the module is in a state where it cannot work at all.
     */
    UNHEALTHY,
    /**
     * UNKNOWN means that the state of the module is unknown, which can only be set by another module.
     */
    UNKNOWN
}
