package de.urbanpulse.dist.jee.upsecurityrealm.exception;

import de.urbanpulse.dist.jee.upsecurityrealm.exception.handler.IncorrectCredentialsExceptionHandler;
import de.urbanpulse.dist.jee.upsecurityrealm.exception.handler.InvalidTimestampExceptionHandler;
import de.urbanpulse.dist.jee.upsecurityrealm.exception.handler.UnknownAccountExceptionHandler;
import de.urbanpulse.dist.jee.upsecurityrealm.exception.handler.UpServletExceptionHandler;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.exception.InvalidTimeStampException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ExceptionMapperHolder {

    private static final Logger LOGGER = Logger.getLogger(ExceptionMapperHolder.class.getName());

    private static final Map<Class, UpServletExceptionHandler> EXCEPTION_HANDLER_MAP = new HashMap<>();

    static {
        EXCEPTION_HANDLER_MAP.put(InvalidTimeStampException.class, new InvalidTimestampExceptionHandler());
        EXCEPTION_HANDLER_MAP.put(UnknownAccountException.class, new UnknownAccountExceptionHandler());
        EXCEPTION_HANDLER_MAP.put(IncorrectCredentialsException.class, new IncorrectCredentialsExceptionHandler());
    }

    private ExceptionMapperHolder() {
    }

    public static UpServletExceptionHandler getHandlerFor(AuthenticationException e) {
        LOGGER.info("Getting handler for exception: " + e.getClass());
        UpServletExceptionHandler upServletExceptionHandler = EXCEPTION_HANDLER_MAP.get(e.getClass());
        if (upServletExceptionHandler == null) {
            LOGGER.warning("No handler found for exception");
            throw e;
        }
        return upServletExceptionHandler;
    }


}
