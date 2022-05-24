package de.urbanpulse.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HmacUtils {

    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String CHARSET = "UTF8";
    private static final Logger LOGGER = Logger.getLogger(HashingUtils.class.getName());

    /**
     * generate HmacSHA256 hash
     *
     * @param key key to use for hashing
     * @param data data to hash
     *
     * @return base64-encoded SHA256 HMAC createBcrypt of the data
     * @throws IllegalArgumentException key is null or empty, or data is null
     * @throws NoSuchAlgorithmException algorithm implementation is not supported
     * @throws InvalidKeyException This is the exception for invalid Keys (invalid encoding, wrong length, uninitialized, etc)
     * @throws UnsupportedEncodingException encoding not supported
     */
    public static String createHmac256(String key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }

        if (data == null) {
            throw new IllegalArgumentException("null data");
        }

        Mac sha256 = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET), HMAC_ALGORITHM);
        sha256.init(keySpec);
        byte[] hashBytes = sha256.doFinal(data);
        LOGGER.log(Level.FINE, "UPUtils HMAC Hasher: {0}", Base64.encodeBase64String(hashBytes));
        return Base64.encodeBase64String(hashBytes);
    }

    /**
     * generate HmacSHA256 hash
     *
     * @param key key to use for hashing
     * @param data data to hash
     *
     * @return base64-encoded SHA256 HMAC createBcrypt of the data
     * @throws IllegalArgumentException key is null or empty, or data is null
     * @throws NoSuchAlgorithmException algorithm implementation is not supported
     * @throws InvalidKeyException This is the exception for invalid Keys (invalid encoding, wrong length, uninitialized, etc)
     * @throws UnsupportedEncodingException encoding not supported
     */
    public static String createHmac256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        if (data == null) {
            throw new IllegalArgumentException("null data");
        }
        return createHmac256(key, data.getBytes(CHARSET));
    }
}
