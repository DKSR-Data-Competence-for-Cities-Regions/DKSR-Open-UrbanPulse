package de.urbanpulse.dist.jee.upsecurityrealm;

import java.util.Base64;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPPasswordServiceTest {

    private static final String VALID_PWD = "myValidPassword";
    private static final String INVALID_PWD = "notMyValidPassword";
    private static final String VALID_HASH = "$2a$10$MX8tA2Dps/jn6o/3wWJ6Q.3b5wsavaijkyrPxb/EL02IjG8YOF7O2";


    @Test
    public void test_doCredentialsMatch_returnsTrue_forValidCredentials() {
        assertTrue(UPPasswordService.doCredentialsMatch(VALID_PWD, VALID_HASH));
    }

    @Test
    public void test_doCredentialsMatch_returnsTrue_forValidCredentials_nonHashedPwd() {
        assertTrue(UPPasswordService.doCredentialsMatch(VALID_PWD, VALID_HASH, false));
    }

    @Test
    public void test_doCredentialsMatch_returnsTrue_forValidCredentials_hashedPwd() {
        String encryptedPwd = new String(Base64.getEncoder().encode(VALID_PWD.getBytes()));
        assertTrue(UPPasswordService.doCredentialsMatch(encryptedPwd, VALID_HASH, true));
    }

    @Test
    public void test_doCredentialsMatch_returnsFalse_forInvalidCredentials() {
        assertFalse(UPPasswordService.doCredentialsMatch(INVALID_PWD, VALID_HASH));
    }

    @Test
    public void test_doCredentialsMatch_returnsFalse_forInvalidCredentials_nonHashedPwd() {
        assertFalse(UPPasswordService.doCredentialsMatch(INVALID_PWD, VALID_HASH, false));
    }

    @Test
    public void test_doCredentialsMatch_returnsFalse_forInvalidCredentials_hashedPwd() {
        String encryptedPwd = new String(Base64.getEncoder().encode(INVALID_PWD.getBytes()));
        assertFalse(UPPasswordService.doCredentialsMatch(encryptedPwd, VALID_HASH, true));
    }

}
