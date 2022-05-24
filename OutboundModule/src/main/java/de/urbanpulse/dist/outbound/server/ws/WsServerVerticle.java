package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.dist.outbound.MainVerticle;
import de.urbanpulse.util.server.HttpServerFactory;
import de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler;

import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_EVENTTYPE_LIVEDATA_READ_TEMPLATE;
import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_SENSOR_SID_LIVEDATA_READ_TEMPLATE;
import de.urbanpulse.dist.util.MessageConsumerUnregistrationHelper;
import de.urbanpulse.dist.util.StatementConsumerManagementVerticle;
import de.urbanpulse.dist.util.UpdateListenerConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Deploy only one instance per OutboundModule!
 * <p>
 * WS server with REST endpoint for debugging.</p>
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsServerVerticle extends StatementConsumerManagementVerticle {

    public static final String SETUP_ADDRESS = WsServerVerticle.class.getName();

    private final MessageConsumerUnregistrationHelper consumerUnregistrationHelper = new MessageConsumerUnregistrationHelper();
    private final Map<String, List<ServerWebSocketWrapper>> statementToWsSessionMap = new HashMap<>();

    private URI wsBaseUrl;
    private URI restBaseUrl;
    private String basePathWithLeadingSlashOnly;
    private String host;
    private String externalHost;
    private Integer portSecure;
    private Integer portInsecure;
    private WsServerTargetMatcher matcher;
    private RestRequestHandler restRequestHandler;

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject config = config();
        host = config.getString("host");
        externalHost = config.getString("externalHost");

        portSecure = config.getInteger("portSecure", -1) != -1 ? config.getInteger("portSecure") : config.getInteger("port", -1);
        portInsecure = config.getInteger("portInsecure", -1);
        basePathWithLeadingSlashOnly = config.getString("basePathWithLeadingSlashOnly");

        setUpBaseURLs(startPromise);

        matcher = new WsServerTargetMatcher();
        restRequestHandler = new RestRequestHandler(matcher, restBaseUrl, statementToListenerMap);

        List<Future> serverFutures = new ArrayList<>();
        if (portSecure != -1) {
            Promise<Void> fS = Promise.promise();
            serverFutures.add(fS.future());
            setUpServer(config, fS, true);
        }
        if (portInsecure != -1) {
            Promise<Void> fI = Promise.promise();
            serverFutures.add(fI.future());
            setUpServer(config, fI, false);
        }
        if (serverFutures.isEmpty()) {
            startPromise.fail("No servers could be started due to missing port configuration");
            return;
        }

        registerSetupConsumer(SETUP_ADDRESS);
        // set prefix for registering consumers
        statementPrefix = MainVerticle.GLOBAL_STATEMENT_PREFIX;

        CompositeFuture.all(serverFutures).onComplete(deplResult -> {
            if (deplResult.succeeded()) {
                logger.info("all servers have been started successfully");
                startPromise.complete();
            } else {
                logger.info("something went wrong during server startup deployment. ", deplResult.cause());
                startPromise.fail(deplResult.cause());
            }
        });
    }

    private void handleRest(RoutingContext routingContext) {
        final String upgradeHeader = routingContext.request().headers().get("Upgrade");
        boolean isWebsocket = "websocket".equalsIgnoreCase(upgradeHeader);
        if (isWebsocket) {
            handleWebSocketConnection(routingContext);
        } else {
            restRequestHandler.handle(routingContext);
        }

    }

    @Override
    protected void registerUpdateListener(UpdateListenerConfig ulConfig) {
        String target = ulConfig.getTarget();
        final String statementName = ulConfig.getStatementName();

        URI targetURL;
        try {
            targetURL = new URI(target);
        } catch (URISyntaxException | RuntimeException ex) {
            throw new IllegalArgumentException("ws server target [" + target + "] malformed URL");
        }
        if (!matcher.matches(targetURL.getPath(), wsBaseUrl.getPath())) {
            throw new IllegalArgumentException("ws server target [" + target + "] unsupported");
        }
        if (!matcher.extractStatement(targetURL.getPath(), wsBaseUrl.getPath()).equals(statementName)) {
            String msg = String.format("ws server target has to match the statement name. target: [%s], statementName: [%s]", target, statementName);
            throw new IllegalArgumentException(msg);
        }
        if (statementToListenerMap.containsKey(statementName)) {
            throw new IllegalStateException("update listener for statement [" + statementName + "] already registered");
        }
    }

    @Override
    protected void handleEvent(String statementName, JsonObject event) {
        statementToWsSessionMap.putIfAbsent(statementName, new LinkedList<>());
        List<ServerWebSocketWrapper> statementSessions = statementToWsSessionMap.get(statementName);
        List<ServerWebSocketWrapper> deadSessions = new LinkedList<>();

        statementSessions.parallelStream().forEach((ServerWebSocketWrapper wsWrapper) -> {
            ServerWebSocket webSocket = wsWrapper.getWebsocket();
            try {
                if (!webSocket.writeQueueFull()) {
                    CompositeFuture.any(checkPermissionsOnEventType(event, wsWrapper.getUser()),
                            checkPermissionsOnSID(event, wsWrapper.getUser()))
                            .onComplete(completeHndlr -> {
                                if (completeHndlr.succeeded()) {
                                    handleAuthorizedResult(wsWrapper, event, statementName);
                                } else {
                                    logger.debug(completeHndlr.cause());
                                }
                            });
                } else {

                    logger.warn("writeQueue is full for target[" + webSocket.remoteAddress().host() + "] of statement[" + statementName + "], will omit this event.");
                }
            } catch (RuntimeException e) {
                logger.error("failed to send to target[" + webSocket.remoteAddress().host() + "] for statement[" + statementName
                        + "], will be removed...");
                deadSessions.add(wsWrapper);
            }
        });

        deadSessions.forEach(statementSessions::remove);
    }

    protected Future<Void> checkPermissionsOnEventType(JsonObject event, User user) {
        Future<Void> eventTypePermissionCheck;
        if (event.containsKey("_headers") && event.getJsonObject("_headers").containsKey("eventType")) {
            String requiredPermission = String.format(PERMISSION_EVENTTYPE_LIVEDATA_READ_TEMPLATE, event.getJsonObject("_headers").getString("eventType"));
            eventTypePermissionCheck = userHasPermission(user, requiredPermission);
        } else {
            eventTypePermissionCheck = Future.failedFuture("No eventType permission!");
        }

        return eventTypePermissionCheck;
    }

    protected Future<Void> checkPermissionsOnSID(JsonObject event, User user) {
        Future<Void> sidPermissionCheck;

        if (event.containsKey("SID")) {
            String requiredPermission = String.format(PERMISSION_SENSOR_SID_LIVEDATA_READ_TEMPLATE, event.getString("SID"));
            sidPermissionCheck = userHasPermission(user, requiredPermission);
        } else {
            sidPermissionCheck = Future.failedFuture("No SID permission!");
        }

        return sidPermissionCheck;
    }


    @Override
    protected void unregisterUpdateListener(UpdateListenerConfig ulConfig) {
        clearSessionsForStatement(ulConfig.getStatementName());
    }

    private void clearSessionsForStatement(String statementName) {
        logger.info("clearing WS sessions for statement [" + statementName + "]...");
        if (statementToWsSessionMap.containsKey(statementName)) {
            List<ServerWebSocketWrapper> sessions = statementToWsSessionMap.get(statementName);
            sessions.forEach(wrappedSession
                    -> wrappedSession.getWebsocket().close()
            );
            statementToWsSessionMap.remove(statementName);
        }
    }

    @Override
    protected void reset() {
        logger.info("resetting WS server...");
        clearSessionsForAllStatements();
    }

    private void clearSessionsForAllStatements() {
        List<String> statements = new LinkedList<>(statementToListenerMap.keySet());
        statements.forEach(this::clearSessionsForStatement);
    }

    private void handleWebSocketConnection(RoutingContext routingContext) {
        HttpServerRequest httpServerRequest = routingContext.request();
        String extractedStatement = matcher.extractStatement(httpServerRequest.path(), basePathWithLeadingSlashOnly);

        if (!statementToListenerMap.containsKey(extractedStatement)) {
            httpServerRequest.response().setStatusCode(400).end("Invalid path.");
            return;
        }

        // Ensure that the same protocol is used
        String dbTarget = statementToListenerMap.get(extractedStatement).iterator().next().getTarget();
        boolean protocolMatches = httpServerRequest.isSSL() ? dbTarget.startsWith("wss://") : dbTarget.startsWith("ws://");
        if (!protocolMatches) {
            httpServerRequest.response().setStatusCode(400).end("Protocol missmatch.");
            return;
        }

        logger.info("WebSocket connection for statement [" + extractedStatement + "] created");

        statementToWsSessionMap.putIfAbsent(extractedStatement, new LinkedList<>());
        List<ServerWebSocketWrapper> statementSessions = statementToWsSessionMap.get(extractedStatement);
        try {
            ServerWebSocket websocket = httpServerRequest.upgrade();
            ServerWebSocketWrapper wsWrapper = new ServerWebSocketWrapper(websocket, routingContext.user());
            statementSessions.add(wsWrapper);
            websocket.closeHandler(ignore -> {
                logger.info("Close handler for WS called, removing session.");
                statementSessions.remove(wsWrapper);
            });
        } catch (IllegalStateException ex) {
            logger.error("Error during websocket handshake", ex);
            httpServerRequest.response().setStatusCode(500).end("Websocket upgrade failed!");
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        statementToWsSessionMap.clear();
        super.stop(stopPromise);
    }

    /**
     * @return the consumerUnregistrationHelper
     */
    public MessageConsumerUnregistrationHelper getConsumerUnregistrationHelper() {
        return consumerUnregistrationHelper;
    }

    /**
     * @return the statementToWsSessionMap
     */
    public Map<String, List<ServerWebSocketWrapper>> getStatementToWsSessionMap() {
        return statementToWsSessionMap;
    }

    private void setUpServer(JsonObject config, Promise<Void> deployment, boolean secure) {
        HttpServer httpsServer = HttpServerFactory.createHttpServer(vertx, config, secure);

        Router router = setupRouter();

        httpsServer.requestHandler(router::accept);

        int port = secure ? portSecure : portInsecure;
        startListening(httpsServer, port, host, deployment);
    }

    private Router setupRouter() {
        Router router = Router.router(vertx);
        ShiroAuthHandler authHandler = new ShiroAuthHandler(vertx);
        router.route().handler(LoggerHandler.create());
        router.get("/*").handler(authHandler::authenticate);
        router.get("/OutboundInterfaces/outbound/*")
                .handler(this::handleRest);
        return router;
    }

    private void startListening(HttpServer httpServer, int port, String host, Promise<Void> listening) {
        logger.info("trying to listen on host[" + host + "] port[" + port + "]...");

        httpServer.listen(port, host, (AsyncResult<HttpServer> asyncResult) -> {
            if (asyncResult.succeeded()) {
                listening.complete();
            } else {
                listening.fail("failed to listen on host[" + host + "] port[" + port + "]");
            }
        });

    }

    private void setUpBaseURLs(Promise<Void> startPromise) {
        String wsBaseUrlString = new StringBuilder("ws").append("://").append(externalHost).append(":")
                .append(portSecure).append(basePathWithLeadingSlashOnly).toString();
        String httpBaseUrlString = new StringBuilder("http").append("://").append(externalHost).append(":")
                .append(portSecure).append(basePathWithLeadingSlashOnly).toString();
        try {
            wsBaseUrl = new URI(wsBaseUrlString);
            restBaseUrl = new URI(httpBaseUrlString);
        } catch (URISyntaxException exception) {
            startPromise.fail(exception);
        }
        logger.debug("base URL set up");
    }

    // We *have* to use "isAuthorized" here because the follow-up API won't be available until
    // vert.x 4.0
    @SuppressWarnings("deprecation")
    protected Future<Void> userHasPermission(User user, String requiredPermission){
        Promise<Void> userHasPermission = Promise.promise();

        user.isAuthorized(requiredPermission, hndlr -> {
            //we use compositefuture which checks if any of the futures has succeeded
            //but here we have boolean futures which transport the result in a boolean variable
            //for the compisitefuture to work we fail the result here on false for the isAuthorized method
            if (hndlr.succeeded() && hndlr.result()) {
                userHasPermission.complete();
            } else {
                userHasPermission.fail("User has permission for " + requiredPermission);
            }
        });

        return userHasPermission.future();
    }

    protected void handleAuthorizedResult(ServerWebSocketWrapper wsWrapper, JsonObject event, String statementName) {
        wsWrapper.getWebsocket().writeFinalTextFrame(event.encode());

    }

}
