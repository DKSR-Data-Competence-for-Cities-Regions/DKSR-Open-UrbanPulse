package de.urbanpulse.backchannel.utils;

/**
 *
 * @author Steffen Haertlein
 */
public class AuthUtils {

    public static boolean isBasicAuthHeader(String authHeader) {
        return authHeader.startsWith("Basic ");
    }

}
