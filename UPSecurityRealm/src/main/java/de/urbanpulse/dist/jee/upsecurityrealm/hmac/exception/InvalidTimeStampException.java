package de.urbanpulse.dist.jee.upsecurityrealm.hmac.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * This exception is thrown if the value of UrbanPulse-Timestamp header is invalid in a request.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class InvalidTimeStampException extends AuthenticationException {

    public InvalidTimeStampException(String message) {
        super(message);
    }

}
