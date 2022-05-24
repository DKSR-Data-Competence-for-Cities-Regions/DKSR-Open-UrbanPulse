package de.urbanpulse.persistence.v3.storage;

import de.urbanpulse.outbound.QueryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TestStorageServiceImpl extends AbstractStorage {

    public TestStorageServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }


    @Override
    public void stop(Handler<AsyncResult<Void>> result) {
        vertx.eventBus().send(config.getString("responseAddress"), "stop");
        result.handle(Future.succeededFuture());
    }

    @Override
    public void start(Handler<AsyncResult<Void>> result) {
        vertx.eventBus().send(config.getString("responseAddress"), "start");
        result.handle(Future.succeededFuture());
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler) {
        vertx.eventBus().send(config.getString("responseAddress"), "query");
        resultHandler.handle(Future.succeededFuture());
    }

    @Override
    public void persist(List<JsonObject> events) {
        vertx.eventBus().send(config.getString("responseAddress"), "persist");
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {

    }

    @Override
    protected String getMetricsPrefix() {
        return "persistence_ts";
    }

}
