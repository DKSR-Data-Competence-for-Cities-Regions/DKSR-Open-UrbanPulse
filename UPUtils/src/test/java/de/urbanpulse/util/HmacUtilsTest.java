package de.urbanpulse.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import static org.hamcrest.CoreMatchers.is;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HmacUtilsTest {

    private final Logger LOGGER = LoggerFactory.getLogger(HmacUtilsTest.class);

    @Test
    public void testCreateHmac256ThrowIlligalKey() {
        try {
            try {
                HmacUtils.createHmac256(null, (byte[]) null);
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException ex) {
                LOGGER.error("SEVERE: createHmac256 throws inner error", ex);
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("null key"));
        }
    }

    @Test
    public void testCreateHmac256ThrowIlligalData() {
        String key = "key";
        try {
            try {
                HmacUtils.createHmac256(key, (byte[]) null);
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException ex) {
                LOGGER.error("SEVERE: createHmac256 throws inner error", ex);
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("null data"));
        }
    }

    @Test
    public void testCreateHmac256() {
        String key = "key";
        String data = "data";

        try {
            String expectedHash = "UDH+PZicbRU3oBP6bnOdojRj/a7DtwE32Cjjas4iG9A=";
            assertEquals(expectedHash, HmacUtils.createHmac256(key, data));
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("SEVERE: createHmac256 throws NoSuchAlgorithmException error", ex);
        } catch (InvalidKeyException ex) {
            LOGGER.error("SEVERE: createHmac256 throws InvalidKeyException error", ex);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("SEVERE: createHmac256 throws UnsupportedEncodingException error", ex);
        }
    }
}
