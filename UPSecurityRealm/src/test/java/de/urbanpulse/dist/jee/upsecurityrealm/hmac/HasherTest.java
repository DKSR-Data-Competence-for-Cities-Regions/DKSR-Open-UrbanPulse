package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class HasherTest {

    private Hasher hasher;

    private static final String DATA = "everypony smile!";
    private static final String EXPECTED_BASE64_HMAC_SHA256 = "ZoEdjQ076iMJHI5zq0K/QpPrAaZz4wN8+sE0HoHL53g=";
    private static final String KEY = "I've got the key, I've got the secret...";

    @Before
    public void setUp() throws Exception {
        hasher = new Hasher();
    }

    @Test
    public void createHmac256_returnsExpectedHash() throws Exception {
        assertEquals(EXPECTED_BASE64_HMAC_SHA256, hasher.createHmac256(KEY, DATA));
    }

    @Test
    public void createHmac256_doesNotReturnEmptyHashForEmptyData() throws Exception {
        assertFalse(hasher.createHmac256(KEY, "").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHmac256_throwsForNullData() throws Exception {
        hasher.createHmac256(KEY, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHmac256_throwsForNullKey() throws Exception {
        hasher.createHmac256(null, DATA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHmac256_throwsForEmptyKey() throws Exception {
        hasher.createHmac256("", DATA);
    }
}
