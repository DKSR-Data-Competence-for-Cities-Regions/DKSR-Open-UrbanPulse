package de.urbanpulse.dist.jee.upsecurityrealm.cors;

import java.io.IOException;
import static java.util.Arrays.stream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.web.servlet.OncePerRequestFilter;

/**
 * CORS filter
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CorsFilter extends OncePerRequestFilter {

    private final static Logger LOG = Logger.getLogger(CorsFilter.class.getName());

    private final String[] allowedOrigins = {"https://.*\\.urbanpulse\\.de", "http://.*\\.swagger\\.io"};

    @Override
    public void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        LOG.info("Checking whether the request is a CORS request...");

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String requestOrigin = httpRequest.getHeader("Origin");
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        if (requestOrigin != null) {
            LOG.log(Level.INFO, "Origin is present: {0}", requestOrigin);
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
            httpResponse.setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Origin", (stream(allowedOrigins).anyMatch(requestOrigin::matches)) ? requestOrigin : "https://*.urbanpulse.de");
        } else {
            LOG.info("Origin is not present in the request");
        }
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            LOG.info("NOT an OPTIONS request, moving to the next filter in the filter chain");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

}
