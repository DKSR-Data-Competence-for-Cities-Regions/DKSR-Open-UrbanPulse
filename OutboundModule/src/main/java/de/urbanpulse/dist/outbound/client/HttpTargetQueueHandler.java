package de.urbanpulse.dist.outbound.client;

import de.urbanpulse.util.upqueue.UPQueueHandler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * sends queued events via HTTP(S)
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class HttpTargetQueueHandler implements UPQueueHandler<JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTargetQueueHandler.class);

    private final HttpClient client;
    private final URI uri;
    private final String path;

    private final EventDataJsonFactory jsonFactory = new EventDataJsonFactory();
    private final JsonObject credentials;

    /**
     * Deprecated. Use
     * {@link #HttpTargetQueueHandler(HttpClient, URI, JsonObject)} instead
     */
    @Deprecated
    public HttpTargetQueueHandler(HttpClient client, String endpoint, JsonObject clientCredentials) {
        this.client = client;
        this.uri = null;
        this.path = endpoint;
        this.credentials = clientCredentials;
    }

    public HttpTargetQueueHandler(HttpClient client, URI uri, JsonObject clientCredentials) {
        this.client = client;
        this.uri = uri;
        this.path = uri.getPath();
        this.credentials = clientCredentials;
    }

    @Override
    public void handle(List<JsonObject> objects) {
        if (objects.isEmpty()) {
            return;
        }

        StringBuilder builder = jsonFactory.buildEventDataJson(objects);

        try {
            HttpClientRequest request = client.putAbs(uri.toString(), (HttpClientResponse response) -> {
                int statusCode = response.statusCode();
                if (statusCode >= 300) {
                    LOGGER.error("Unable to connect, HTTP status code: " + statusCode + ", URI: " + (uri != null ? uri : path)+ " "+response.statusMessage());
                }
            }).putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            if (credentials != null) {
                if ("BASIC".equals(credentials.getString("authMethod"))) {
                    addBasicAuthHeader(request);
                    request.end(builder.toString(), "UTF-8");
                } else if (credentials.getString("hmacKey") != null) {
                    LOGGER.info("Old credentials format used - using fallback");
                    request.end(builder.toString(), "UTF-8");
                } else if (credentials.getString("authMethod") != null) {
                    LOGGER.warn("Unsupported auth method: " + credentials.getString("authMethod"));
                } else {
                    LOGGER.error("Invalid credentials: " + credentials);
                }
            } else {
                LOGGER.error("http authorization error: invalid credentials.");
            }
        } catch (IllegalStateException e) {
            // can happen on undeploy because we use a raw thread to run this, as the client is potentially already closed
            LOGGER.error(new StringBuilder("failed to post: ").append(e.getMessage()).toString());
        }
    }

    private void addBasicAuthHeader(HttpClientRequest request) {
        String username = credentials.getString("user");
        String password = credentials.getString("password");
        try {
            byte[] base64 = Base64.getEncoder().encode((username + ":" + password).getBytes("UTF-8"));
            request.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(base64, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(new StringBuilder("failed to encode basic auth credentials: ").append(ex.getMessage()).toString());
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
