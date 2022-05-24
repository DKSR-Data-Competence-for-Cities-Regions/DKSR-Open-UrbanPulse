package de.urbanpulse.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HashingUtils {
    public static String hash(String textToHash) {
        return BCrypt.hashpw(textToHash, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
