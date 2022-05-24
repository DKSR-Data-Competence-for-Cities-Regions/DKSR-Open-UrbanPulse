package de.urbanpulse.dist.inbound.http;

import de.urbanpulse.dist.inbound.http.auth.MessageAuthenticator;
import de.urbanpulse.dist.inbound.http.auth.MessageTimestampValidator;
import de.urbanpulse.dist.inbound.http.auth.UPAuthHeaderDecoder;
import de.urbanpulse.util.AccessLogger;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import de.urbanpulse.util.server.HttpServerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpReceiverVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpReceiverVerticle.class);
    private static final AccessLogger ACCESS_LOGGER = AccessLogger.newInstance(HttpReceiverVerticle.class);
    private static final int NO_CONTENT = 204;
    private static final int BAD_REQUEST = 400;
    private static final int UNAUTHORIZED = 401;
    private static final int METHOD_NOT_ALLOWED = 405;

    public static final String VERTICLE_NAME = HttpReceiverVerticle.class.getName();

    private LocalMap<String, String> connectorAuthMap;
    private LocalMap<String, String> eventTypesMap;

    private HttpServer httpServer;
    private MessageTimestampValidator timestampValidator;
    private MessageAuthenticator messageAuthenticator;

    private Counter totalEventsReceivedCounter;
    private Counter totalBytesReceivedCounter;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        vertx.deployVerticle(VertxQueue.class, new DeploymentOptions().setConfig(config()));


        timestampValidator = new MessageTimestampValidator();

        connectorAuthMap = vertx.sharedData().getLocalMap(HttpInboundCommandHandler.CONNECTOR_AUTH_MAP_NAME);
        eventTypesMap = vertx.sharedData().getLocalMap(HttpInboundCommandHandler.SENSOR_EVENT_TYPES_MAP_NAME);

        messageAuthenticator = new MessageAuthenticator(new UPAuthHeaderDecoder(), connectorAuthMap);

        final JsonObject config = config();

        Promise<HttpServer> httpServerPromise = Promise.promise();

        startHttpServer(config).onComplete(httpServerPromise);

        httpServerPromise.future().onSuccess(server -> {
            httpServer = server;
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }

    private void registerMeters(MeterRegistry registry) {
        if (registry != null) {
            totalEventsReceivedCounter = Counter.builder("up_inbound_http_events_received")
                    .description("Number of events received.")
                    .register(registry);
            totalBytesReceivedCounter = Counter.builder("up_inbound_http_bytes_received")
                    .description("Number of bytes received.")
                    .register(registry);
        }
    }

    private Future<HttpServer> startHttpServer(JsonObject config) {
        Promise<HttpServer> result = Promise.promise();
        HttpServer server = HttpServerFactory.createHttpServer(vertx, config);

        server.requestHandler(request -> {
            ACCESS_LOGGER.log(request);
            handleRequest(request, config);
        });

        String host = config.getString("host");
        int port = config.getInteger("port");
        server.listen(port, host, result);
        return result.future();
    }

    private void handleRequest(HttpServerRequest request, JsonObject config) {
        if (!validateRequestHasCorrectMethodAndExpectedContent(request)) {
            return;
        }

        boolean enforceHmac = config.getBoolean("enforceHmac", true);
        boolean enforceTimestamp = config.getBoolean("enforceTimestamp", true);

        String timestamp = request.getHeader("UrbanPulse-Timestamp");
        String authorization = request.getHeader("Authorization");
        request.bodyHandler((Buffer buffer) -> {

            incTotalBytesCounter(buffer.length());

            if (enforceTimestamp && timestampValidator.isInvalid(timestamp)) {
                String message = "invalid UrbanPulse-Timestamp header, must be within 15 minutes of servertime";
                request.response().setStatusCode(BAD_REQUEST).end(message);
                return;
            }

            String body = buffer.toString("UTF-8");
            final String dataToHash = timestamp + body;
            if (enforceHmac && !messageAuthenticator.isAuthenticated(authorization, dataToHash)) {
                String message = "invalid credentials for HMAC auth on " + request.absoluteURI() + " for header[" + authorization
                        + "] and data[" + dataToHash.substring(0, (dataToHash.length() > 50) ? 50 : dataToHash.length() - 1) + "]";
                request.response().setStatusCode(UNAUTHORIZED).end(message);
                return;
            }

            JsonArray eventsArray = new JsonObject(body).getJsonArray("data");
            eventsArray.stream().map(JsonObject.class::cast).forEach(event -> {
                String sid = event.getString("SID");
                String eventType = eventTypesMap.get(sid);
                if (null != eventType) {
                    JsonObject headers = event.getJsonObject("_headers", new JsonObject()).put("eventType", eventType);
                    event.put("_headers", headers);
                    incTotalEventsCounter();
                } else {
                    LOGGER.error("eventType for SID " + sid + " is empty!");
                }
            });

            vertx.eventBus().send("queue", eventsArray);
            setRefTimestamp(request.response());
            request.response().setStatusCode(NO_CONTENT).end();
        });
    }

    private boolean validateRequestHasCorrectMethodAndExpectedContent(HttpServerRequest request) {
        HttpMethod method = request.method();
        if (HttpMethod.GET.equals(method) && request.absoluteURI().contains("ready")) {
            request.response().end();
            return false;
        }

        if (!HttpMethod.PUT.equals(method) && !HttpMethod.POST.equals(method)) {
            String message = "unsupported method " + method + ", use PUT or POST instead";
            request.response().setStatusCode(METHOD_NOT_ALLOWED).end(message);
            return false;
        }

        String contentType = request.getHeader("Content-Type");
        if (!"application/json".equalsIgnoreCase(contentType)) {
            String message = "unsupported content type " + contentType + ", must be application/json";
            request.response().setStatusCode(BAD_REQUEST).end(message);
            return false;
        }

        return true;
    }

    private void setRefTimestamp(HttpServerResponse response) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(now);
        response.putHeader("ref-timestamp", timestamp);
    }

    @Override
    public void stop() throws Exception {
        httpServer.close();
        super.stop();
    }

    private void incTotalEventsCounter() {
        if (totalEventsReceivedCounter != null) {
            totalEventsReceivedCounter.increment();
        }
    }

    private void incTotalBytesCounter(double amount) {
        if (totalBytesReceivedCounter != null) {
            totalBytesReceivedCounter.increment(amount);
        }
    }

}
