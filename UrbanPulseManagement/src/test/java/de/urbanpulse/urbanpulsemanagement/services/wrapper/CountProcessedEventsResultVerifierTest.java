package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CountProcessedEventsResultVerifierTest {

    private CountProcessedEventsResultVerifier verifier;

    @Before
    public void setUp() {
        verifier = new CountProcessedEventsResultVerifier();
    }

    @Test
    public void bug3546regressionTest() {
        assertTrue(verifier.verify(new JsonObject(
                "{\"header\":{\"senderId\":\"296edcc4-fdec-425d-b6bb-1a46334f31c4\",\"messageSN\":2,\"inReplyTo\":2},\"body\":{\"processedEvents\":110193632}}")));
        assertFalse(verifier.verify(new JsonObject(
                "{\"header\":{\"senderId\":\"296edcc4-fdec-425d-b6bb-1a46334f31c4\",\"messageSN\":2,\"inReplyTo\":2},\"body\":{\"foobar\":4711}}")));
        assertFalse(verifier.verify(new JsonObject()));
        assertFalse(verifier.verify(null));
    }

}
