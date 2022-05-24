package de.urbanpulse.dist.outbound.client;

import de.urbanpulse.util.upqueue.UPQueueHandler;
import io.vertx.core.MultiMap;
import static io.vertx.core.MultiMap.caseInsensitiveMultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

/**
 * sends queued events via WebSocket (plain or encrypted)
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsTargetQueueHandler implements UPQueueHandler<JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsTargetQueueHandler.class);

    private static final long MIN_RECONNECT_DELAY = 1000;
    private static final long MAX_RECONNECT_DELAY = 1000 * 60 * 30;

    private final Vertx vertx;

    private final HttpClient client;

    private final String hmacKey;

    private WebSocket webSocket;
    private boolean done;
    private long reconnectDelay;
    private long reconnectTimer;

    private final EventDataJsonFactory jsonFactory = new EventDataJsonFactory();
    private final JsonObject credentials;

    public WsTargetQueueHandler(Vertx vertx, HttpClient client, String endpoint, JsonObject listenerCredentials) {
        this.vertx = vertx;
        this.client = client;
        this.hmacKey = listenerCredentials.getString("hmacKey");
        this.reconnectDelay = MIN_RECONNECT_DELAY;

        LOGGER.info("WsTargetQueueHandler scheduling connect in 1 sec");
        this.vertx.setTimer(1000, (Long event) -> {
            LOGGER.info("WsTargetQueueHandler connecting to websocket");
            connect(client, endpoint);
        });
        this.credentials = listenerCredentials;
    }

    private void connect(HttpClient client, String endpoint) {
        LOGGER.info("connecting to websocket " + endpoint);
        MultiMap headers = caseInsensitiveMultiMap();
        if (credentials != null && "BASIC".equals(credentials.getString("authMethod"))) {
            addBasicAuthHeader(headers);

            client.websocket(endpoint, headers, (WebSocket ws) -> {
                reconnectDelay = MIN_RECONNECT_DELAY;
                LOGGER.info("connected on websocket " + ws);
                webSocket = ws;
                webSocket.closeHandler((Void blank) -> {
                    LOGGER.info("websocket was closed remotely");
                    webSocket = null;
                    reconnect(client, endpoint);
                });
            }, (Throwable error) -> {
                LOGGER.info("websocket connection error: " + error.getMessage());
                webSocket = null;
                reconnect(client, endpoint);
            });
        }
        else{
            LOGGER.error(new StringBuilder("websocket authorization error: invalid or no basic auth credentials."));
        }
    }

    private void addBasicAuthHeader(MultiMap headers) {
        String username = credentials.getString("user");
        String password = credentials.getString("password");
        try {
            byte[] base64 = Base64.getEncoder().encode((username + ":" + password).getBytes("UTF-8"));
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(base64, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(new StringBuilder("failed to encode basic auth credentials: ").append(ex.getMessage()).toString());
        }
    }

    private void reconnect(HttpClient client, String endpoint) {
        if (done) {
            LOGGER.info("already done, not reconnecting");
            return;
        }

        LOGGER.info("reconnecting in " + reconnectDelay + " msec...");

        reconnectTimer = vertx.setTimer(reconnectDelay, (Long event) -> {
            reconnectTimer = 0;
            connect(client, endpoint);
        });

        reconnectDelay = Math.min(MAX_RECONNECT_DELAY, reconnectDelay * 2);
    }

    @Override
    public void handle(List<JsonObject> objects) {
        if (objects.isEmpty()) {
            return;
        }

        if (webSocket == null) {
            LOGGER.info("websocket not yet open...skipping " + objects.size() + " objects");
            return;
        }

        StringBuilder builder = jsonFactory.buildEventDataJson(objects);
        webSocket.writeFinalTextFrame(builder.toString());
    }

    @Override
    public void close() {
        done = true;

        if (reconnectTimer != 0) {
            vertx.cancelTimer(reconnectTimer);
            reconnectTimer = 0;
        }

        if (webSocket != null) {
            webSocket.close();
        }

        client.close();
    }
}
