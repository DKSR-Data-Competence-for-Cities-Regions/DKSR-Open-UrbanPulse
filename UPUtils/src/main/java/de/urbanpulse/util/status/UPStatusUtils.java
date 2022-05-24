package de.urbanpulse.util.status;

import io.vertx.circuitbreaker.CircuitBreakerState;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPStatusUtils {

    private UPStatusUtils() {

    }

    public static UPModuleState fromCircuitBreakerState(CircuitBreakerState state) {
        switch (state) {
            case OPEN:
                return UPModuleState.UNHEALTHY;
            case HALF_OPEN:
                return UPModuleState.UNSTABLE;
            case CLOSED:
                return UPModuleState.HEALTHY;
            default:
                return UPModuleState.UNHEALTHY;
        }
    }

}
