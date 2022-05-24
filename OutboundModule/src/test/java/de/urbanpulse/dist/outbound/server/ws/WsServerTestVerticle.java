package de.urbanpulse.dist.outbound.server.ws;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Optional;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsServerTestVerticle extends AbstractVerticle {

    private WsServerTargetMatcher matcher;

    private HttpServer httpServer;

    private RestRequestHandler restRequestHandler;
    private String host;
    private int port;

    @Override
    public void start() throws Exception {
        super.start();
        matcher = new WsServerTargetMatcher();
        JsonObject config = new JsonObject();
        host = "localhost";
        port = 4711;
        String basePathWithLeadingSlashOnly = "/test";
        String wsBaseUrl = "ws://" + host + ":" + port + basePathWithLeadingSlashOnly;
        String restBaseUrlString = "http://" + host + ":" + port + basePathWithLeadingSlashOnly;
        URI restBaseUrl = new URI(restBaseUrlString);
        restRequestHandler = new RestRequestHandler(matcher, restBaseUrl, new HashMap<>());

        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(false);
        httpServer = vertx.createHttpServer(options);
        Router router = Router.router(vertx);
        router.route().handler(routingContext -> {
            final String upgradeHeader = routingContext.request().headers().get("Upgrade");
            Boolean isWebsocket = Optional.ofNullable(upgradeHeader).map(s -> "websocket".equals(s.toLowerCase())).orElse(false);
            if (isWebsocket) {
                handleWebSocket(routingContext.request().upgrade());
            } else {
               handleRest(routingContext);
            }
        });
        httpServer.requestHandler(router::accept);
        httpServer.listen(port, host);
    }

    @Override
    public void stop() throws Exception {
        httpServer.close((AsyncResult<Void> event) -> {
            try {
                super.stop();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void handleRest(RoutingContext context) {
        restRequestHandler.handle(context);
    }

    private void handleWebSocket(ServerWebSocket webSocket) {
        String path = webSocket.path();
        String requestedTarget = "ws://" + host + ":" + port + path;

        boolean validPath = true;
        String statementName = "myStatement";

        if (validPath) {
        } else {
            webSocket.reject();
        }
    }
}
