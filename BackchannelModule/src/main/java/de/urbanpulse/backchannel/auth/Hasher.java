package de.urbanpulse.backchannel.auth;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

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
     * @throws NoSuchAlgorithmException can't find the algorithm that is needed
     * @throws InvalidKeyException  the key is invalid
     * @throws UnsupportedEncodingException  the encoding is not supported
     */
    public String createHmac256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
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
        Logger.getLogger(Hasher.class.getName()).log(Level.FINE, "UrbanPulse Hasher: {0}", Base64.encodeBase64String(hashBytes));
        return Base64.encodeBase64String(hashBytes);
    }
}
