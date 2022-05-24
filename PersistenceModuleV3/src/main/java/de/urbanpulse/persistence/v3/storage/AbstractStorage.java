package de.urbanpulse.persistence.v3.storage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.Optional;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractStorage implements StorageService {

    public static final int DEFAULT_MAX_CACHED_EVENTS_PER_SID = 100;
    public static final ServiceException UNSUPPORTED_OPERATION = new ServiceException(405, "Unsupported Operation");

    protected final Vertx vertx;
    protected final JsonObject config;
    protected Counter totalEventsPersistedCounter;

    public AbstractStorage(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

    }

    public abstract void stop(Handler<AsyncResult<Void>> result);

    public abstract void start(Handler<AsyncResult<Void>> result);

    protected abstract void registerAdditionalMeters(MeterRegistry registry);

    protected abstract String getMetricsPrefix();

    protected void registerMeters(MeterRegistry registry) {
        totalEventsPersistedCounter = Counter.builder(getMetricsPrefix() + "_events_persisted")
                .description("Number of events persisted.")
                .register(registry);
        registerAdditionalMeters(registry);
    }

    protected void incTotalEventsPersistedCounter(double amount) {
        if (totalEventsPersistedCounter != null) {
            totalEventsPersistedCounter.increment(amount);
        }
    }

}
