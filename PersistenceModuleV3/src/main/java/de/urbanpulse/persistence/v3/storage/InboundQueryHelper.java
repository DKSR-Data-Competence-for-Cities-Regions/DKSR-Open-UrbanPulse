package de.urbanpulse.persistence.v3.storage;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class InboundQueryHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundQueryHelper.class);

    Vertx vertx;
    JsonObject config;
    long pullDelay;
    private Handler<List<JsonObject>> handler;

    public InboundQueryHelper(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        JsonObject secondLevelConfig
                = config.getJsonObject("secondLevelConfig", new JsonObject());
        this.pullDelay = secondLevelConfig.getLong("pullDelay", 100L);
    }

    public void startPulling(Handler<List<JsonObject>> handler) {
        this.handler = handler;
        queryInbound();
    }

    private void queryInbound() {
        String pullAddress = config.getString("pullAddress", "pullAddress");
        if (pullAddress == null) {
            LOGGER.error("pullAddress is null");
            return;
        }
        vertx.eventBus().request(pullAddress, null, asyncResult -> {
            if (asyncResult.succeeded()) {
                Message<Object> result = asyncResult.result();
                handlePullResult(result.body());
            } else {
                LOGGER.warn(
                        "pulling events from inbound failed: " + asyncResult.cause().getMessage());
                vertx.setTimer(pullDelay, i -> queryInbound());
            }
        });
    }

    private void handlePullResult(Object resultBody) {
        if (resultBody instanceof JsonArray && !((JsonArray) resultBody).isEmpty()) {

            JsonArray resultList = (JsonArray) resultBody;
            List<JsonObject> jsonObjects = resultList.stream().filter(JsonObject.class::isInstance).map(JsonObject.class::cast).collect(Collectors.toList());

            handler.handle(jsonObjects);
            vertx.runOnContext(ctx -> queryInbound());
        } else {
            vertx.setTimer(pullDelay, i -> queryInbound());
        }
    }

}
