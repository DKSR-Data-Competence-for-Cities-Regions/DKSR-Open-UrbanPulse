package de.urbanpulse.util;

import de.urbanpulse.util.status.UPModuleState;
import static de.urbanpulse.util.status.UPStatusUtils.fromCircuitBreakerState;
import io.vertx.circuitbreaker.CircuitBreakerState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPStatusUtilsTest {

    @Test
    public void test_fromCircuitBreakerState_returnsCorrectValues() {
        assertEquals(UPModuleState.UNHEALTHY, fromCircuitBreakerState(CircuitBreakerState.OPEN));
        assertEquals(UPModuleState.UNSTABLE, fromCircuitBreakerState(CircuitBreakerState.HALF_OPEN));
        assertEquals(UPModuleState.HEALTHY, fromCircuitBreakerState(CircuitBreakerState.CLOSED));
    }

}
