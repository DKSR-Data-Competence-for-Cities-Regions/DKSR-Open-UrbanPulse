package de.urbanpulse.dist.outbound.server.historicaldata;

import de.urbanpulse.outbound.PersistenceQueryService;
import de.urbanpulse.outbound.QueryConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class PersistenceQueryServiceImplMockWithBackPressure implements PersistenceQueryService {

    private static final int BATCH_SIZE = 20;

    private final Vertx vertx;
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceQueryServiceImplMockWithBackPressure.class);
    private final JsonArray preparedBatch = new JsonArray();
    private JsonObject message;

    public PersistenceQueryServiceImplMockWithBackPressure(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle,
            Handler<AsyncResult<Void>> resultHandler) {
        LOGGER.info("Request received!");

        for (int i = 0; i < BATCH_SIZE; i++) {
            preparedBatch.add(new JsonObject());
        }
        prepareAndSendMessage(uniqueRequestHandle, resultHandler);
    }

    private void prepareAndSendMessage(String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler) {
        message = new JsonObject().put("batch", preparedBatch);

        resultHandler.handle(Future.succeededFuture());

        vertx.setTimer(100, l -> {
            sendBatch(uniqueRequestHandle);
        });
    }

    private void sendBatch(String uniqueRequestHandle) {
        vertx.eventBus().request(uniqueRequestHandle, message, response -> {
            if(response.succeeded() && ((String)response.result().body()).equals("sendNextBatch")) {
                LOGGER.info("Next batch requested for: "+uniqueRequestHandle);
                message.put("isLast", Boolean.TRUE);
                sendBatch(uniqueRequestHandle);
            }
        });
    }

}
