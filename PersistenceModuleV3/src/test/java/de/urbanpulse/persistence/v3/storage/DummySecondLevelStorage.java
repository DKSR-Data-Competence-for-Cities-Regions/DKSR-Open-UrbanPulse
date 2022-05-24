package de.urbanpulse.persistence.v3.storage;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class DummySecondLevelStorage extends AbstractSecondLevelStorage {

    public DummySecondLevelStorage(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    Future<Iterator<JsonObject>> queryLatest(String sid) {
        return null;
    }

    @Override
    Future<Iterator<JsonObject>> query(String sid, ZonedDateTime since, ZonedDateTime until) {
        return null;
    }

    @Override
    public void stop(Handler<AsyncResult<Void>> result) {
        result.handle(Future.succeededFuture());
    }

    @Override
    public void start(Handler<AsyncResult<Void>> result) {
        result.handle(Future.succeededFuture());
    }

    @Override
    public void persist(List<JsonObject> events) {
        //we don't need to persist anything
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {

    }

    @Override
    protected String getMetricsPrefix() {
        return "persistence_dummy";
    }
}
