package de.urbanpulse.dist.outbound.client;

import de.urbanpulse.dist.outbound.MainVerticle;
import de.urbanpulse.dist.util.upqueue.OutboundQueueWorkerFactory;
import de.urbanpulse.util.upqueue.UPQueue;
import de.urbanpulse.util.upqueue.UPQueueHandler;
import de.urbanpulse.util.upqueue.UPQueueImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.net.URI;

/**
 * queue and bulk-send events via mechanisms supported by the {@link HttpClient}
 * (HTTP/WebSocket)
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpVerticle.class);
    private UPQueue upQueue;

    @Override
    public void start() {
        String target = config().getString("target");
        String statementName = config().getString("statementName");
        JsonObject listenerCredentials = config().getJsonObject("credentials");
        JsonObject clientConfig = config().getJsonObject("clientConfig");
        boolean trustAll = clientConfig.getBoolean("trustAll");
        int queueWorkerCount = clientConfig.getInteger("queueWorkerCount");
        int queueBatchSize = clientConfig.getInteger("queueBatchSize");
        int queueCapacity = clientConfig.getInteger("queueCapacity");

        //for backwards compatibility. If credentials wasn't set via SetupMaster, but in the config of OutboundModule
        if (listenerCredentials.getString("authMethod") == null) {
            listenerCredentials = getCredentialsFromConfig(listenerCredentials.getString("hmacKey"));
        }
        upQueue = createQueue(trustAll, target, listenerCredentials, queueWorkerCount, queueBatchSize, queueCapacity);
        LOGGER.info("created client");

        String localStatementAddress = MainVerticle.LOCAL_STATEMENT_PREFIX + statementName;
        vertx.eventBus().localConsumer(localStatementAddress, (Message<JsonObject> event) -> {
            upQueue.addMessage(event.body());
        });
    }

    @Override
    public void stop() {
        upQueue.flush();
        upQueue.close();
        // Vert.x will automatically unregister any handlers when the verticle is stopped.
    }

    @Deprecated
    //for backwards compatibility. If credentials wasn't set via SetupMaster, but in the config of OutboundModule
    private JsonObject getCredentialsFromConfig(String hmacKey) {
        JsonObject oldCredentials = config().getJsonObject("clientConfig").getJsonObject("credentials");
        JsonObject credentials = new JsonObject();
        if (oldCredentials != null) {
            credentials.put("authMethod", "BASIC");
            credentials.put("user", oldCredentials.getString("basicAuthUsername"));
            credentials.put("password", oldCredentials.getString("basicAuthPassword"));
        }
        credentials.put("hmacKey", hmacKey);
        return credentials;
    }

    private UPQueue createQueue(boolean trustAll, String target, JsonObject listenerCredentials, int queueWorkerCount,
            int queueBatchSize, int queueCapacity) {
        URI uri = URI.create(target);
        String endpoint = uri.getPath();
        String host = uri.getHost();
        int port = uri.getPort();
        String scheme = uri.getScheme();

        if (port == -1) {
            switch (scheme) {
                case "http":
                    port = 80;
                    break;
                case "https":
                    port = 443;
                    break;
                case "clientws":
                    port = 80;
                    break;
                case "clientwss":
                    port = 443;
                    break;
                default:
                    LOGGER.error("failed to detect default port for [" + scheme + "]");
                    break;
            }
        }

        boolean encrypted = "https".equalsIgnoreCase(scheme) || "clientwss".equalsIgnoreCase(scheme);
        boolean isWebSocket = "clientws".equalsIgnoreCase(scheme) || "clientwss".equalsIgnoreCase(scheme);

        HttpClient client = createClient(trustAll, host, port, encrypted, isWebSocket, vertx);

        UPQueueHandler<JsonObject> targetQueueHandler;
        if (isWebSocket) {
            LOGGER.info("creating WS target queue handler for target " + target + " with scheme " + scheme);
            targetQueueHandler = new WsTargetQueueHandler(vertx, client, endpoint, listenerCredentials);
        } else {
            LOGGER.info("creating HTTP target queue handler for target " + target + " with scheme " + scheme);
            targetQueueHandler = new HttpTargetQueueHandler(client, uri, listenerCredentials);
        }

        return new UPQueueImpl<>(new OutboundQueueWorkerFactory<>(), targetQueueHandler, queueWorkerCount, queueBatchSize,
                queueCapacity);
    }

    private HttpClient createClient(boolean trustAll, String host, int port, boolean encrypted, boolean isWebSocket, Vertx vertx) {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(host);
        options.setDefaultPort(port);
        options.setSsl(encrypted);
        options.setTrustAll(trustAll);
        options.setConnectTimeout(10000);
        if (!isWebSocket) {
            // (http-)keepAlive is true by default, only a client-side option, and not to be confused with TCP keepalive
            // (see http://tldp.org/HOWTO/TCP-Keepalive-HOWTO/overview.html), which is false by default and applies to
            // both server and client)
            options.setKeepAlive(true);
            options.setPipelining(true);
        }
        return vertx.createHttpClient(options);
    }

}
