package de.urbanpulse.eventbus.helpers;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EventBusImplementationDefaultsHolderTest {

    @Test
    public void testToString() {
        assertEquals("de.urbanpulse.eventbus.vertx.VertxEventbusFactory",
                EventBusImplementationDefaultsHolder.DEFAULT_EVENTBUS_FACTORY.toString());
    }
}
