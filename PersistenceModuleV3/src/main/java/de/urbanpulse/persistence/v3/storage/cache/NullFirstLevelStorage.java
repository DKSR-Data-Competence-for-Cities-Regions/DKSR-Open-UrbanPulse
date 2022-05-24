package de.urbanpulse.persistence.v3.storage.cache;

import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.storage.AbstractStorage;
import io.micrometer.core.instrument.MeterRegistry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;

/**
 * dummy first-level storage that does not do any actual caching,
 * can be used as fallback to disable first-level storage in case of issues
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class NullFirstLevelStorage extends AbstractStorage {

    private final static Logger LOGGER = LoggerFactory.getLogger(NullFirstLevelStorage.class);

    public NullFirstLevelStorage(Vertx vertx, JsonObject storageConfig) {
        super(vertx, storageConfig);
        LOGGER.warn("!!! first level storage disabled - using null implementation - events will NOT be cached !!!");
    }

    /**
     * this implementation always calls the handler with a succeeded future
     *
     * @param handler an AsyncResult Handler to handler the response later
     */
    @Override
    public void start(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
    }

    /**
     * this implementation always calls the handler with a succeeded future
     *
     * @param handler an AsyncResult Handler to handler the response later
     */
    @Override
    public void stop(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler) {
        resultHandler.handle(Future.failedFuture(UNSUPPORTED_OPERATION));
    }

    /**
     * this implementation does nothing
     *
     * @param events the List of JsonObject representing the events
     */
    @Override
    public void persist(List<JsonObject> events) {
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {
        //Currently no further meters to be registerd. Please add new meters here
    }

    @Override
    protected String getMetricsPrefix() {
        return "up_persistence_firstlevel_null";
    }

}
