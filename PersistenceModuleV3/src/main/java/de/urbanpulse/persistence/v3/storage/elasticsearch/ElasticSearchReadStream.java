package de.urbanpulse.persistence.v3.storage.elasticsearch;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import java.time.LocalDateTime;

import de.urbanpulse.persistence.v3.storage.elasticsearch.helpers.ElasticSearchSearchAfterHelper;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchReadStream implements ReadStream<JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchReadStream.class);

    private final int batchSize;
    private final ElasticSearchSearchAfterHelper searchAfterHighLevelClient;

    private Handler<Void> endHandler;
    private Handler<JsonObject> eventHandler;

    private String upHash;
    private boolean isLast;
    private boolean paused;
    private long amountOfDocumentsLeft;
    private long since;

    public ElasticSearchReadStream(int batchSize, ElasticSearchSearchAfterHelper searchAfterHighLevelClient,
            long amountOfDocumentsExpected, long since) {
        this.batchSize = batchSize;
        this.isLast = false;
        this.paused = false;
        this.searchAfterHighLevelClient = searchAfterHighLevelClient;
        this.amountOfDocumentsLeft = amountOfDocumentsExpected;
        this.since = since;
        this.upHash = null; //we don't have the hash at the beginning

        endHandler = v -> LOGGER.debug("Batch data sent at {0}", LocalDateTime.now());
    }

    private void requestData() {
        if (amountOfDocumentsLeft > 0) {
            runSearchAfterQuery();
        } else {
            endHandler.handle(null);
        }
    }

    private void runSearchAfterQuery() {
        searchAfterHighLevelClient.searchAfterQuery(since, upHash, batchSize).setHandler(result -> {
            if (result.succeeded()) {
                LOGGER.debug("++++++++++ Started working on batch at {0}", LocalDateTime.now());
                JsonArray batchData = result.result().getJsonArray("batchData");
                extractAnHandleMetaDataFromResponse(result.result(), batchData.size());
                sendData(batchData);
            } else {
                LOGGER.error("No documents received from ElasticSearch! Sending last message...", result.cause());
                //something went wrong with elasticserch so we send this as the last message
                //and set everything to finished
                amountOfDocumentsLeft = 0;
                isLast = true;
                sendData(new JsonArray());
            }
        });
    }

    private void extractAnHandleMetaDataFromResponse(JsonObject responseObject, int batchDataSize) {
        amountOfDocumentsLeft = amountOfDocumentsLeft - batchDataSize;

        since = responseObject.getLong("lastTimeStamp", 0L);
        upHash = responseObject.getString(ElasticSearchSecondLevelStorageServiceImpl.UP_HASH_FIELD_NAME, null);

        if (amountOfDocumentsLeft <= 0) {
            isLast = true;
        }

        if (amountOfDocumentsLeft < 0) {
            LOGGER.warn("Received more documents than expected! Exceeded expected amount with {0}",
                    Math.abs(amountOfDocumentsLeft));
        }

        if (since == 0) {
            LOGGER.warn("Received a zero timestamp!");
            LOGGER.debug("amountOfDocumentsLeft are {0}", amountOfDocumentsLeft);
            LOGGER.debug("isPaused {0} ", paused);
            amountOfDocumentsLeft = 0;
            isLast = true;
        }
    }

    private void sendData(JsonArray batchData) {
        JsonObject sensorData = new JsonObject();

        sensorData.put("batch", batchData);
        sensorData.put("isLast", isLast);
        sensorData.put("batchTimestamp", LocalDateTime.now().toString());

        LOGGER.debug("Sending batch at {0}", LocalDateTime.now());
        eventHandler.handle(sensorData);

        if (isLast) {
            LOGGER.info("Finished sending data at {0}", LocalDateTime.now());
        }

        if (!paused) {
            requestData();
        }
    }

    @Override
    public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
       //exception handler ignored for now, currently no need for it
        return this;
    }

    @Override
    public ReadStream<JsonObject> handler(@Nullable Handler<JsonObject> handler) {
        eventHandler = handler;
        requestData();
        return this;
    }

    @Override
    public ReadStream<JsonObject> pause() {
        this.paused = true;
        return this;
    }

    @Override
    public ReadStream<JsonObject> resume() {
        this.paused = false;
        requestData();
        return this;
    }

    @Override
    public ReadStream<JsonObject> endHandler(@Nullable Handler<Void> handler) {
        endHandler = handler;
        return this;
    }

    @Override
    public ReadStream<JsonObject> fetch(long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Pipe<JsonObject> pipe() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pipeTo(WriteStream<JsonObject> stream) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pipeTo(WriteStream<JsonObject> stream, Handler<AsyncResult<Void>> hndlr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
