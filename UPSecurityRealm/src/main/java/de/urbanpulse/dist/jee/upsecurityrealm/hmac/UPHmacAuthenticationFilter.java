package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import de.urbanpulse.dist.jee.upsecurityrealm.exception.ExceptionMapperHolder;
import de.urbanpulse.dist.jee.upsecurityrealm.exception.handler.UpServletExceptionHandler;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPHmacAuthenticationFilter extends AuthenticatingFilter {

    private static final Logger LOGGER = Logger.getLogger(UPHmacAuthenticationFilter.class.getName());
    private static final String MISSING_CREDENTIALS_EXCEPTION_MESSAGE = "User name and/or password is missing from Authorization header";
    private static final String MISSING_PRINCIPALS_EXCEPTION_MESSAGE = "Principals or credentials are missing";

    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String UP_TIMESTAMP_HEADER = "UrbanPulse-Timestamp";
    protected static final String UPCONNECTOR_AUTH_SCHEMA = "UPCONNECTOR";
    protected static final String UPUSER_AUTH_SCHEMA = "UP";
    protected static final String BASIC_AUTH_SCHEMA = "BASIC";
    protected static final String BEARER_AUTH_SCHEMA = "BEARER";

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return false;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse sr1) throws Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        request = new ServletRequestBodyWrapper(httpRequest);
        UPAuthMode authorizationMode = getAuthzMode(request);
        if (authorizationMode == null) {
            LOGGER.severe("Authorization mode cannot be extracted from request");
            return null;
        }
        String authorizationHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
        String authMode = getAuthMode(authorizationHeader);
        LOGGER.info("Creating token with auth mode: " + authMode);
        if ((BASIC_AUTH_SCHEMA).equalsIgnoreCase(authMode)) {
            String userNameAndPassword = Base64.decodeToString(authorizationHeader.split(" ")[1]);
            String[] credentials = userNameAndPassword.split(":");
            if (credentials.length < 2) {
                throw new IncorrectCredentialsException(MISSING_CREDENTIALS_EXCEPTION_MESSAGE);
            }
            return new UsernamePasswordToken(credentials[0], credentials[1], false);
        }

        String[] prinCred = getPrincipalsAndCredentials(authorizationHeader, request);
        if (prinCred == null || prinCred.length < 2) {
            throw new IncorrectCredentialsException(MISSING_PRINCIPALS_EXCEPTION_MESSAGE);
        }

        String accessKeyId = prinCred[0];
        String signature = prinCred[1];

        return createToken(accessKeyId, signature, request, sr1);
    }

    @Override
    protected AuthenticationToken createToken(String accessKeyId, String signature, ServletRequest request, ServletResponse response) {
        return new HmacToken(accessKeyId, signature, request, getAuthzMode(request));
    }

    protected String getAuthMode(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String[] authTokens = authorizationHeader.split(" ");
        if (authTokens == null || authTokens.length < 2) {
            return null;
        }
        return authTokens[0];
    }

    protected String[] getPrincipalsAndCredentials(String authorizationHeader, ServletRequest request) {
        if (authorizationHeader == null) {
            return null;
        }
        String[] authTokens = authorizationHeader.split(" ");
        if (authTokens == null || authTokens.length < 2) {
            return null;
        }
        return getPrincipalsAndCredentials(authTokens[0], authTokens[1]);
    }

    protected String[] getPrincipalsAndCredentials(String scheme, String encoded) {
        return encoded.split(":", 2);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        boolean loggedIn = false;
        if (isLoginAttempt(request, response)) {
            try {
                loggedIn = executeLogin(request, response);
            } catch (IllegalStateException e) {
                // This happens if no user or password was provided
                return false;
            }
        } else {
            challengeLogin(response);
        }
        return loggedIn;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        LOGGER.info("Login failed");
        UpServletExceptionHandler exceptionHandler = ExceptionMapperHolder.getHandlerFor(e);
        boolean exceptionHandled = exceptionHandler.handle(response, e);
        if (!exceptionHandled) {
            challengeLogin(response);
        }
        return false;
    }

    private void challengeLogin(ServletResponse response) {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"UrbanPulse\"");
    }

    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        UPAuthMode authzMode = getAuthzMode(request);
        return authzMode != null && isLoginAttempt(authzMode);
    }

    protected boolean isLoginAttempt(UPAuthMode authzMode) {
        return authzMode.equals(UPAuthMode.UP) || authzMode.equals(UPAuthMode.UPCONNECTOR) ||
                authzMode.equals(UPAuthMode.BASIC) || authzMode.equals(UPAuthMode.BEARER);
    }

    protected UPAuthMode getAuthzMode(ServletRequest request) {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null) {
            return null;
        }
        String mode = authHeader.split(" ")[0].toUpperCase();
        if (mode.equals(UPAuthMode.UP.name())) {
            return UPAuthMode.UP;
        } else if (mode.equals(UPAuthMode.UPCONNECTOR.name())) {
            return UPAuthMode.UPCONNECTOR;
        } else if (mode.equals(UPAuthMode.BASIC.name())) {
            return UPAuthMode.BASIC;
        } else if (mode.equals(UPAuthMode.BEARER.name())){
          return UPAuthMode.BEARER;
        } else {
            return null;
        }
    }

    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        AuthenticationToken token = null;
        try {
            token = this.createToken(request, response);
            try {
                Subject subject = this.getSubject(request, response);
                subject.login(token);
                return this.onLoginSuccess(token, subject, request, response);
            } catch (AuthenticationException var5) {
                return this.onLoginFailure(token, var5, request, response);
            }
        } catch (AuthenticationException ex) {
            return this.onLoginFailure(token, ex, request, response);
        }
    }
}
