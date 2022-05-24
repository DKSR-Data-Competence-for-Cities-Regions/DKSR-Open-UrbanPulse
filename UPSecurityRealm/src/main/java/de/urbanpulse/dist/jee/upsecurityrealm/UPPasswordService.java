package de.urbanpulse.dist.jee.upsecurityrealm;

import java.util.Base64;
import org.apache.shiro.authc.credential.PasswordService;
import org.mindrot.jbcrypt.BCrypt;
/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPPasswordService implements PasswordService {

    @Override
    public String encryptPassword(Object plainPassword) throws IllegalArgumentException {
        if (plainPassword instanceof String) {
            String password = (String) plainPassword;
            return BCrypt.hashpw(password, BCrypt.gensalt());
        }
        throw new IllegalArgumentException("Invalid plain credential!");

    }

    @Override
    public boolean passwordsMatch(Object submittedPlaintext, String encrypted) {
        if (submittedPlaintext instanceof char[]) {
            String password = String.valueOf((char[]) submittedPlaintext);
            return doCredentialsMatch(password, encrypted);
        }
        throw new IllegalArgumentException(
                "passwordsMatch only support char[] credential!");
    }

    public static boolean doCredentialsMatch(String submittedPwd, String encryptedPwd) {
        return doCredentialsMatch(submittedPwd, encryptedPwd, false);
    }

    public static boolean doCredentialsMatch(String submittedPwd, String encryptedPwd, boolean pwdIsBase64) {
        if (pwdIsBase64) {
            submittedPwd = new String(Base64.getDecoder().decode(submittedPwd));
        }
        return BCrypt.checkpw(submittedPwd, encryptedPwd);
    }

}
