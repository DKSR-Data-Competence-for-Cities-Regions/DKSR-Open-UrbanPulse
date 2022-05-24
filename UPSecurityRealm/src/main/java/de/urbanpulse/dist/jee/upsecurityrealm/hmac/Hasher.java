package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class Hasher {

    public static final String ALGORITHM = "HmacSHA256";
    public static final String CHARSET = "UTF8";

    /**
     * generate HmacSHA256 hash
     *
     * @param key key to use for hashing
     * @param data data to hash
     *
     * @return base64-encoded SHA256 HMAC hash of the data
     * @throws IllegalArgumentException key is null or empty, or data is null
     * @throws NoSuchAlgorithmException the hashing algorithm is not existing
     * @throws InvalidKeyException the key is not correct
     * @throws UnsupportedEncodingException could not find the encoding on the system
     */
    public static String createHmac256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }

        if (data == null) {
            throw new IllegalArgumentException("null data");
        }

        Mac sha256 = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
        sha256.init(keySpec);
        byte[] hashBytes = sha256.doFinal(data.getBytes(CHARSET));
        Logger.getLogger(Hasher.class.getName()).log(Level.FINEST, "UrbanPulse Hasher: {0}", Base64.encodeBase64String(hashBytes));
        return Base64.encodeBase64String(hashBytes);
    }

    /**
     * @return random SHA 256 HMAC key
     * @throws NoSuchAlgorithmException  HmacSHA256 not supported
     */
    public static String generateRandomHmacSha256Key() throws NoSuchAlgorithmException {
        KeyGenerator hmacSha256generator = KeyGenerator.getInstance(ALGORITHM);
        SecretKey secret = hmacSha256generator.generateKey();
        byte[] encodedKeyBytes = secret.getEncoded();
        return Base64.encodeBase64String(encodedKeyBytes);
    }
}
