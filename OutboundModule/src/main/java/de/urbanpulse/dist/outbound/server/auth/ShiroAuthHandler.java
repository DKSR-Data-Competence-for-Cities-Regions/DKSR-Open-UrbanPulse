package de.urbanpulse.dist.outbound.server.auth;

import de.urbanpulse.auth.upsecurityrealm.shiro.UpShiroAuth;
import io.vertx.core.AsyncResult;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import static java.util.Objects.isNull;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ShiroAuthHandler {

    public static final String PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE = "sensor:%s:historicdata:read";
    public static final String PERMISSION_EVENTTYPE_HISTORICDATA_READ_TEMPLATE = "eventtype:%s:historicdata:read";
    public static final String PERMISSION_SENSOR_SID_LIVEDATA_READ_TEMPLATE = "sensor:%s:livedata:read";
    public static final String PERMISSION_EVENTTYPE_LIVEDATA_READ_TEMPLATE = "eventtype:%s:livedata:read";

    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroAuthHandler.class);

    private final Vertx vertx;
    private final UpShiroAuth upShiroAuth;
    private final Long clearCachePeriod;

    public ShiroAuthHandler(Vertx vertx) {
        this.vertx = vertx;
        this.upShiroAuth = new UpShiroAuth(this.vertx, SecurityUtils.getSecurityManager());
        this.clearCachePeriod = this.vertx.getOrCreateContext().config().getLong("clearCachePeriod", 30000L);
        vertx.setPeriodic(clearCachePeriod, this::clearCache);
    }

    public void authenticate(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        upShiroAuth.authenticate(authorization, loginResult -> {
            User user = handleLogin(loginResult, request);
            if (user != null) {
                routingContext.setUser(user);
                routingContext.next();
            }
        });
    }

    public void authorize(RoutingContext routingContext, String requiredPermission) {
        HttpServerRequest request = routingContext.request();
        routingContext.user().isAuthorized(requiredPermission, authResult -> {
            if (authResult.failed()) {
                String msg = "Internal error during authorization attempt.";
                LOGGER.error(msg, authResult.cause());
                sendAuthFailureResponse(request, 500, msg);
            }
            else if (Boolean.FALSE.equals(authResult.result())) {
                sendAuthFailureResponse(request, 403, "Not authorized.");
            } else {
                routingContext.next();
            }
        });
    }

    public void clearCache(Long l) {
        ((DefaultSecurityManager) upShiroAuth.getSecurityManager()).getRealms()
                .stream()
                .map(AuthorizingRealm.class::cast)
                .forEach(realm -> realm.getAuthorizationCache().clear());
    }

    protected User handleLogin(AsyncResult<User> loginResult, HttpServerRequest request) {
        if (loginResult.failed()) {
            final String msg = "Invalid credentials.";
            sendAuthFailureResponse(request, 401, msg);
            return null;
        }
        User user = loginResult.result();
        if (isNull(user)) {
            final String msg = "Internal error during login attempt.";
            LOGGER.error(msg);
            sendAuthFailureResponse(request, 500, msg);
        }
        return user;
    }

    private void sendAuthFailureResponse(HttpServerRequest request, int statusCode, String message) {
        request.response()
                .setStatusCode(statusCode)
                .putHeader("WWW-Authenticate", "Basic realm=\"UrbanPulse\"")
                .end(message);
    }


}
