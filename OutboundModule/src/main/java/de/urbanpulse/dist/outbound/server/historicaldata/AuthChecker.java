
package de.urbanpulse.dist.outbound.server.historicaldata;

import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_EVENTTYPE_HISTORICDATA_READ_TEMPLATE;
import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class used by {@link HistoricalDataRestVerticle}, provides functionality for
 * authorizing and authenticating users vs their respective sensor permissions.
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class AuthChecker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthChecker.class.getName());

    private final JsonObject rulesConfigs;
    private final JsonObject rolesToRulesConfigurations;

    AuthChecker(JsonObject rulesConfigs, JsonObject rolesToRulesConfigurations) {
        this.rulesConfigs = rulesConfigs;
        this.rolesToRulesConfigurations = rolesToRulesConfigurations;
    }

    /**
     * Check a user's role against the rolesToRulesConfiguration from the config.
     *
     * @param user
     * @param rulesConfigs
     * @param rolesToRulesConfigurations
     * @return a Future that will be completed with a corresponding rule for that user
     */
    Future<JsonObject> checkRole(User user) {
        Promise<JsonObject> filterConfig = Promise.promise();
        // We cannot avoid usage of raw type future here because of a current limitation in vert.x
        // See: https://github.com/eclipse-vertx/vert.x/issues/2058
        @SuppressWarnings("java:S3740")
        List<Future> futureList = rolesToRulesConfigurations.stream()
                .map(roleToRule -> isUserAuthorizedForRole(user, roleToRule.getKey(),
                        (String) roleToRule.getValue()))
                .collect(Collectors.toList());
        CompositeFuture.any(futureList).onComplete(handler -> {
            if (handler.succeeded()) {
                for (Future<String> future : futureList) {
                    if (future.succeeded()) {
                        filterConfig.complete(rulesConfigs.getJsonObject(future.result()));
                        break;
                    }
                }
            } else {
                filterConfig.complete(rulesConfigs.getJsonObject("default_rule"));
            }
        });
        return filterConfig.future();
    }

    // We *have* to use "isAuthorized" here because the follow-up API won't be available until
    // vert.x 4.0
    @SuppressWarnings("deprecation")
    private Future<String> isUserAuthorizedForRole(User user, String role, String rule) {
        Promise<String> promise = Promise.promise();
        user.isAuthorized("roles:" + role, auth -> {
            if (auth.succeeded() && auth.result()) {
                LOGGER.info("Checking for role: " + role + " succeeded!");
                promise.complete(rule);
            } else {
                promise.fail("User doesn't have the role:" + role);
            }
        });
        return promise.future();
    }


    @SuppressWarnings("deprecation")
    void filterPermittedSIDs(List<String> requestedSIDs, RoutingContext routingContext,
                             Handler<AsyncResult<List<String>>> permittedSIDsHandler) {
        if (requestedSIDs.isEmpty()) {
            callSidHandler(permittedSIDsHandler, Future.succeededFuture(requestedSIDs));
        } else if (requestedSIDs.size() == 1) {
            handleSingleSensor(requestedSIDs, routingContext, permittedSIDsHandler);
        } else {
            handleMultipleSensors(requestedSIDs, routingContext, permittedSIDsHandler);
        }
    }

    /*
     * We want to handle two cases
     *  - The user has a permission on the Event -> he can see all of the SIDs
     *  - The user has no permission on the Event:
     *    - User might have an individual permission on one (or more) of the SIDs
     *    - User hase no permission for this Event and its SIDs
     * */
    @SuppressWarnings("deprecation")
    void filterPermittedSIDsAndEventType(List<String> requestedSIDs, RoutingContext routingContext,
                                         String eventType, Handler<AsyncResult<List<String>>> permittedSIDsHandler) {
        if (requestedSIDs.isEmpty()) {
            callSidHandler(permittedSIDsHandler, Future.succeededFuture(requestedSIDs));
        }

        String requiredPermissionEventType =
                String.format(PERMISSION_EVENTTYPE_HISTORICDATA_READ_TEMPLATE, eventType);
        routingContext.user().isAuthorized(requiredPermissionEventType, result -> {
            if (result.succeeded() && result.result()) {
                callSidHandler(permittedSIDsHandler, Future.succeededFuture(requestedSIDs));
            } else {
                filterPermittedSIDs(requestedSIDs, routingContext, permittedSIDsHandler);
            }
        });
    }

    // We *have* to use "isAuthorized" here because the follow-up API won't be available until
    // vert.x 4.0
    @SuppressWarnings("deprecation")
    private void handleMultipleSensors(List<String> requestedSIDs, RoutingContext routingContext, Handler<AsyncResult<List<String>>> permittedSIDsHandler) {
        List<String> authorizedSIDs = new LinkedList<>();
        List<String> unauthorizedSIDs = new LinkedList<>();
        requestedSIDs.forEach(sid -> {
            String requiredPermission =
                    String.format(PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE, sid);
            routingContext.user().isAuthorized(requiredPermission, res -> {
                if (res.succeeded() && res.result()) {
                    authorizedSIDs.add(sid);
                } else {
                    unauthorizedSIDs.add(sid);
                }
                if (authorizedSIDs.size() + unauthorizedSIDs.size() == requestedSIDs.size()) {
                    callSidHandler(permittedSIDsHandler, Future.succeededFuture(authorizedSIDs));
                }
            });
        });
    }

    // We *have* to use "isAuthorized" here because the follow-up API won't be available until
    // vert.x 4.0
    @SuppressWarnings("deprecation")
    private void handleSingleSensor(List<String> requestedSIDs, RoutingContext routingContext, Handler<AsyncResult<List<String>>> permittedSIDsHandler) {
        String sid = requestedSIDs.get(0);
        String requiredPermission = String.format(PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE, sid);
        routingContext.user().isAuthorized(requiredPermission, res -> {
            if (res.succeeded() && res.result()) {
                callSidHandler(permittedSIDsHandler, Future.succeededFuture(Collections.singletonList(sid)));
            } else {
                callSidHandler(permittedSIDsHandler, Future.failedFuture("The provided user does not have permissions for this operation"));
            }
        });
    }

    private void callSidHandler(Handler<AsyncResult<List<String>>> permittedSIDsHandler, Future<List<String>> listFuture) {
        permittedSIDsHandler.handle(listFuture);
    }
}
