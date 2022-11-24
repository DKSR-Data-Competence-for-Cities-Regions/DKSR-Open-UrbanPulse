package de.urbanpulse.backchannel.pojos;

import de.urbanpulse.backchannel.utils.AuthUtils;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Steffen Haertlein
 */
public abstract class Credential {
    protected static final Logger LOGGER = Logger.getLogger(Credential.class.getName());

    public static Credential fromAuthHeader(String authHeader) throws UnsupportedOperationException {
        if (AuthUtils.isBasicAuthHeader(authHeader)) {
            return getBasicAuthCredentials(authHeader.split(" ")[1]);
        }
        throw new UnsupportedOperationException("Authorization method not supported.");
    }

    private static UsernamePasswordCredential getBasicAuthCredentials(String base64encoded) {
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64encoded));
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Cannot decode base64 String: {0}", base64encoded);
            return null;
        }
        Integer colonIndex = decoded.indexOf(":");
        if (colonIndex < 0) {
            return null;
        }
        try {
            String user = decoded.substring(0, colonIndex);
            String pw = decoded.substring(colonIndex + 1);
            return new UsernamePasswordCredential(user, pw);
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.log(Level.WARNING, "Invalid format: {0}, should be 'username:password'", decoded);
            return null;
        }
    }
}
