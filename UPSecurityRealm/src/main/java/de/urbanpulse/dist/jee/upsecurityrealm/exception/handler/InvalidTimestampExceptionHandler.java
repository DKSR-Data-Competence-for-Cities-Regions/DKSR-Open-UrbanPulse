package de.urbanpulse.dist.jee.upsecurityrealm.exception.handler;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.exception.InvalidTimeStampException;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class InvalidTimestampExceptionHandler implements UpServletExceptionHandler<InvalidTimeStampException> {

    private static final Logger LOG = Logger.getLogger(InvalidTimestampExceptionHandler.class.getName());

    @Override
    public boolean handle(ServletResponse response, InvalidTimeStampException ex) {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        LOG.log(Level.SEVERE, "InvalidTimeStampException is caught: {0}", ex.getMessage());
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.addHeader(CAUSE, "Invalid timestamp");
        return true;
    }
}
