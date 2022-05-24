package de.urbanpulse.dist.inbound.http.auth;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;

/**
 * extract hash and decoded username from HTTP Authorization header used in UP message authentication
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class UPAuthHeaderDecoder {

    private static final int USERNAME_GROUP_INDEX = 2;
    private static final int HASH_GROUP_INDEX = 3;
    private static final String CHARSET = "UTF8";
    private static final String REGEX = "\\AUP(Connector)? ([a-zA-Z0-9/+=]+):([a-zA-Z0-9/+=]+)\\z";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    /**
     * get decoded username from HTTP Authorization header
     *
     * @param authHeader HTTP Authorization header (we expect the format "UP base64UTF8username:base64hmacSha256hash")
     * @return decoded username (NOT base64 anymore!)
     * @throws IllegalArgumentException authHeader is not in the expected format
     */
    public String getDecodedUsername(String authHeader) {
        try {
            String base64Username = getMatchingGroup(authHeader, USERNAME_GROUP_INDEX);
            byte[] decodedBytes = Base64.decodeBase64(base64Username);
            return new String(decodedBytes, CHARSET);
        } catch (RuntimeException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException("failed to parse auth header " + authHeader, e);
        }
    }

    /**
     * get hash from HTTP Authorization header
     *
     * @param authHeader HTTP Authorization header (we expect the format "UP base64UTF8username:base64hmacSha256hash")
     * @return hash (still base64-encoded!)
     * @throws IllegalArgumentException authHeader is not in the expected format
     */
    public String getHash(String authHeader) {
        try {
            String hash = getMatchingGroup(authHeader, HASH_GROUP_INDEX);
            return hash;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("failed to parse auth header " + authHeader, e);
        }
    }

    private String getMatchingGroup(String authHeader, int groupIndex) {
        Matcher matcher = PATTERN.matcher(authHeader);
        if (matcher.find()) {
            return matcher.group(groupIndex);
        } else {
            throw new IllegalArgumentException("no match found");
        }
    }
}
