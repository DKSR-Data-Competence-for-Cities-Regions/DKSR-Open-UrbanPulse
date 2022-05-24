package de.urbanpulse.persistence.v3.storage;


import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.outbound.BatchSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
abstract class AbstractSecondLevelStorage extends AbstractStorage {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final int BATCH_SIZE = 500;

    String querySecondLevelAddress;

    protected BatchSender batchSender;

    public AbstractSecondLevelStorage(Vertx vertx, JsonObject config) {
        super(vertx,config);

        logger.info("starting " + getClass());

        new InboundQueryHelper(vertx, config).startPulling(this::persist);

        batchSender = new BatchSender(vertx);
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> queryStartedHandler) {
        queryStartedHandler.handle(Future.succeededFuture());

        if (queryConfig.getSids().isEmpty()) {
            // Nothing to do here - return empty result instantly
            batchSender.sendIteratorResultsInBatches(Collections.emptyIterator(), uniqueRequestHandle);
            return;
        }

        ZonedDateTime since = queryConfig.getSince() != null
                ? ZonedDateTime.from(
                        UPDateTimeFormat.getFormatterWithZoneZ().parse(queryConfig.getSince()))
                : null;
        ZonedDateTime until = queryConfig.getUntil() != null
                ? ZonedDateTime.from(
                        UPDateTimeFormat.getFormatterWithZoneZ().parse(queryConfig.getUntil()))
                : null;

        queryNext(new LinkedList<>(queryConfig.getSids()), uniqueRequestHandle, since, until);
    }

    void queryNext(Queue<String> sidQueue, String uniqueRequestHandle, ZonedDateTime since,
            ZonedDateTime until) {
        boolean isQueryLatest = since == null && until == null;
        String sid = sidQueue.poll();
        logger.info("Querying for SID {0}, ", sid);
        Promise<Void> sidPromise = Promise.promise();
        Future<Iterator<JsonObject>> nextFiterator =
                isQueryLatest ? queryLatest(sid) : query(sid, since, until);
        nextFiterator.onComplete(iteratorResult -> {
            if (iteratorResult.succeeded()) {
                logger.debug("Succeeded querying for SID {0} - streaming to client", sid);
                batchSender.sendIteratorResultsInBatches(iteratorResult.result(),
                        BATCH_SIZE, uniqueRequestHandle, !sidQueue.isEmpty(), sidPromise);
            } else {
                logger.error("Failed querying for SID {0}", iteratorResult.cause(), sid);
                sidPromise.fail(iteratorResult.cause());
            }
        });
        sidPromise.future().onComplete(i -> {
            if (!sidQueue.isEmpty()) {
                queryNext(sidQueue, uniqueRequestHandle, since, until);
            }
        });
    }

    abstract Future<Iterator<JsonObject>> queryLatest(String sid);

    abstract Future<Iterator<JsonObject>> query(String sid, ZonedDateTime since,
            ZonedDateTime until);

}
