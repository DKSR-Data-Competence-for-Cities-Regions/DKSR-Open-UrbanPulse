package de.urbanpulse.dist.inbound.http.auth;

import de.urbanpulse.util.HmacUtils;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class MessageAuthenticator {

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageAuthenticator.class);

    private final UPAuthHeaderDecoder authHeaderDecoder;
    private final LocalMap<String, String> connectorAuth;

    public MessageAuthenticator(UPAuthHeaderDecoder authHeaderDecoder, LocalMap<String, String> connectorAuth) {
        this.authHeaderDecoder = authHeaderDecoder;
        this.connectorAuth = connectorAuth;
    }

    /**
     * check if a message is authenticated with a valid hash
     *
     * @param authHeader HTTP Authorization header (we expect the format "UP
     * base64UTF8username:base64hmacSha256hash")
     * @param dataToHash e.g. timestamp + HTTP body in case of POST/PUT or
     * timestamp + full path in case of GET/DELETE)
     * @return true if authenticated, false if not or an error happened
     */
    public boolean isAuthenticated(String authHeader, String dataToHash) {
        try {
            String username = authHeaderDecoder.getDecodedUsername(authHeader);
            String hash = authHeaderDecoder.getHash(authHeader);
            String key = connectorAuth.get(username);
            if (key == null) {
                return false;
            }

            String expectedHash = HmacUtils.createHmac256(key, dataToHash);
            if (!expectedHash.equals(hash)) {
                LOGGER.error("Username: {0} \n Hash {1} \n Expected {2}", new Object[]{username, hash, expectedHash});
                return false;
            } else {
                return true;
            }

        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException | RuntimeException e) {
            LOGGER.error("error during authentication", e);
            return false;
        }
    }
}
