package de.urbanpulse.dist.jee.upsecurityrealm.exception.handler;

import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.shiro.authc.UnknownAccountException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UnknownAccountExceptionHandler implements UpServletExceptionHandler<UnknownAccountException> {

    private static final Logger LOG = Logger.getLogger(UnknownAccountExceptionHandler.class.getName());

    @Override
    public boolean handle(ServletResponse response, UnknownAccountException ex) {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        LOG.log(Level.SEVERE, "UnknownAccountException is caught: {0}", ex.getMessage());
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.addHeader(CAUSE, "Incorrect credentials");
        return true;
    }
}
