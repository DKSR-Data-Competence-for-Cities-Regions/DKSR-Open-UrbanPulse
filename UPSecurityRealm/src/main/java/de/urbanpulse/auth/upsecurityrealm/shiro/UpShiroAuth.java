package de.urbanpulse.auth.upsecurityrealm.shiro;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.ShiroAuth;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import static java.util.Objects.isNull;

/**
 * An implementation of {@link ShiroAuth} with a constructor that takes a Shiro {@link SecurityManager}.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UpShiroAuth implements ShiroAuth {

    private final SecurityManager securityManager;
    private String rolePrefix = DEFAULT_ROLE_PREFIX;
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String BEARER_AUTH_PREFIX = "Bearer ";
    private static final String BEARER_KEY = "bearer";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private Vertx vertx;

    public UpShiroAuth(Vertx vertx, SecurityManager securityManager) {
        this(vertx, securityManager, null);
    }

    public UpShiroAuth(Vertx vertx, SecurityManager securityManager, String rolePrefix) {
        this.securityManager = securityManager;
        this.vertx = vertx;
        if (!isNull(rolePrefix)) {
            this.rolePrefix = rolePrefix;
        }
    }

    @Override
    public ShiroAuth setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
        return this;
    }

    public SecurityManager getSecurityManager() {
        return this.securityManager;
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
        vertx.executeBlocking(fut -> {
            SubjectContext subjectContext = new DefaultSubjectContext();
            Subject subject = securityManager.createSubject(subjectContext);

            try {
                subject.login(createAuthenticationTokenToken(authInfo));
            } catch (AuthenticationException e) {
                throw new VertxException(e);
            }
            fut.complete(new UpShiroUser(vertx, securityManager, subject, rolePrefix, "roles:"));
        }, resultHandler);
    }

    /**
     * Convenience method that takes a Http basic auth string instead of an authInfo.
     *
     * @param authString
     * @param resultHandler
     */
    public void authenticate(String authString, Handler<AsyncResult<User>> resultHandler) {
        if (securityManager.getClass().equals(UnsecurityManager.class)) {
            SubjectContext subjectContext = new DefaultSubjectContext();
            Subject subject = securityManager.createSubject(subjectContext);
            resultHandler.handle(Future.succeededFuture(new UpShiroUser(vertx, securityManager, subject, rolePrefix, rolePrefix)));
            return;
        }
        if (isNull(authString)) {
            resultHandler.handle(Future.failedFuture("no basic auth string found"));
            return;
        }

        if (authString.contains(BASIC_AUTH_PREFIX)) {
            String base64UsernameColonPw = authString.substring(BASIC_AUTH_PREFIX.length());
            String usernameColonPassword = new String(Base64.decodeBase64(base64UsernameColonPw), StandardCharsets.UTF_8);
            String[] split = usernameColonPassword.split(":", 2);
            if (split.length != 2) {
                resultHandler.handle(Future.failedFuture("couldn't parse the 'Authorization' header"));
                return;
            }
            String username = split[0];
            String password = split[1];
            this.authenticate(new JsonObject().put(USERNAME_KEY, username).put(PASSWORD_KEY, password), resultHandler);
        } else if (authString.contains(BEARER_AUTH_PREFIX)){
            String extractedBearerToken = authString.substring(BEARER_AUTH_PREFIX.length());
            this.authenticate(new JsonObject().put(BEARER_KEY, extractedBearerToken),resultHandler);
        }
    }


    private AuthenticationToken createAuthenticationTokenToken(JsonObject authInfo){
        if (authInfo.containsKey(USERNAME_KEY)){
            return new UsernamePasswordToken(authInfo.getString(USERNAME_KEY), authInfo.getString(PASSWORD_KEY));
        }

        return null;
    }
}
