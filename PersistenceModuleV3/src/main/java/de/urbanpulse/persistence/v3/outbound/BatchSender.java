package de.urbanpulse.persistence.v3.outbound;

import java.time.LocalDateTime;
import java.util.Iterator;

import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class BatchSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSender.class);
    private static final String SEND_NEXT_BATCH = "sendNextBatch";
    public static final int BATCH_SIZE = 500;

    private final Vertx vertx;

    public BatchSender(Vertx vertx) {
        this.vertx = vertx;
    }

    public void sendIteratorResultsInBatches(Iterator<JsonObject> iterator,
            String uniqueRequestHandle) {
        sendIteratorResultsInBatches(iterator, BATCH_SIZE, uniqueRequestHandle, false, noop -> {
        });
    }

    public void sendIteratorResultsInBatches(Iterator<JsonObject> iterator,
            String uniqueRequestHandle, boolean suppressIsLast,
            Handler<AsyncResult<Void>> completeHandler) {
        sendIteratorResultsInBatches(iterator, BATCH_SIZE, uniqueRequestHandle, suppressIsLast,
                completeHandler);
    }

    public void sendIteratorResultsInBatches(Iterator<JsonObject> iterator, int batchSize,
            String uniqueRequestHandle, boolean suppressIsLast) {
        sendIteratorResultsInBatches(iterator, batchSize, uniqueRequestHandle, suppressIsLast,
                noop -> {
                });
    }

    /**
     * iterate across retrieved events and send them in batches over the eventBus
     *
     * @param iterator            iterator
     * @param batchSize           max count of events per batch
     * @param uniqueRequestHandle unique address on the vert.x eventBus associated with this query
     *                            which will receive the batches
     * @param completeHandler     an AsyncResultHandler to handle the result asynchronously later
     * @param suppressIsLast      if true, no isLast message is sent [useful if you want to
     *                            immediately send another iterator (e.g. for another sid) on the
     *                            same stream]
     */
    public void sendIteratorResultsInBatches(Iterator<JsonObject> iterator, int batchSize,
            String uniqueRequestHandle, boolean suppressIsLast,
            Handler<AsyncResult<Void>> completeHandler) {
        vertx.executeBlocking((Promise<JsonObject> e) -> prepareBatch(iterator, batchSize,
                suppressIsLast, uniqueRequestHandle, e), (AsyncResult<JsonObject> result) -> {
                    if (result.succeeded()) {
                        sendBatch(result.result(), uniqueRequestHandle, iterator, batchSize,
                                suppressIsLast, completeHandler);
                    } else {
                        completeHandler.handle(Future.failedFuture(result.cause()));
                    }
                });
    }

    private void prepareBatch(Iterator<JsonObject> iterator, int batchSize, boolean suppressIsLast,
            String uniqueRequestHandle, Promise<JsonObject> e) {
        int i = 0;
        JsonArray currentBatchEvents = new JsonArray();
        try {
            boolean hasNext = iterator.hasNext();
            while (i < batchSize && hasNext) {
                JsonObject event = iterator.next();
                currentBatchEvents.add(event);
                i++;
                hasNext = iterator.hasNext();
            }
            JsonObject message = new JsonObject();
            message.put("batch", currentBatchEvents);
            if (!hasNext && !suppressIsLast) {
                LOGGER.debug("sending last batch to " + uniqueRequestHandle);
                message.put("isLast", true);
            } else {
                LOGGER.debug("sending batch to " + uniqueRequestHandle);
            }
            message.put("batchTimestamp", LocalDateTime.now().toString());
            e.complete(message);
        } catch (Exception ex) {
            LOGGER.error("Sending iterator result failed", ex);
            e.fail(ex);
        }
    }

    private void sendBatch(JsonObject batch, String uniqueRequestHandle,
            Iterator<JsonObject> iterator, int batchSize, boolean suppressIsLast,
            Handler<AsyncResult<Void>> completeHandler) {
        vertx.eventBus().request(uniqueRequestHandle, batch,
                (AsyncResult<Message<String>> response) -> {
                    if (response.succeeded()) {
                        if (response.result().body().equals(SEND_NEXT_BATCH)) {
                            LOGGER.info(
                                    "Next batch request received from : " + uniqueRequestHandle);
                            if (iterator.hasNext()) {
                                sendIteratorResultsInBatches(iterator, batchSize,
                                        uniqueRequestHandle, suppressIsLast, completeHandler);
                            } else {
                                completeHandler.handle(Future.succeededFuture());
                            }
                        }
                    } else {
                        completeHandler.handle(Future.failedFuture(response.cause()));
                    }
                });
    }
}
