package de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier;

import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class DefaultCommandResultVerifierTest {

    private CommandResultVerifier verifier;

    @Before
    public void setUp() {
        verifier = new DefaultCommandResultVerifier();
    }

    @Test
    public void verifyChecksBodyForErrorField_bug3046() {
        JsonObject resultWithoutErrorInBody = new JsonObject().put("body", new JsonObject());

        assertTrue(verifier.verify(resultWithoutErrorInBody));

        JsonObject resultWithErrorInBody = new JsonObject().put("body", new JsonObject().put("error", "something bad happened"));

        assertFalse(verifier.verify(resultWithErrorInBody));
    }

}
