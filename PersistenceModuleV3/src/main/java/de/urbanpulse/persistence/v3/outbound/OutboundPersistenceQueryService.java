package de.urbanpulse.persistence.v3.outbound;

import de.urbanpulse.outbound.PersistenceQueryService;
import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.storage.StorageService;
import static de.urbanpulse.persistence.v3.storage.cache.FirstLevelStorageConst.FIRST_LEVEL_STORAGE_SERVICE_ADDRESS;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundPersistenceQueryService implements PersistenceQueryService {

    private final static Logger LOGGER = LoggerFactory.getLogger(OutboundPersistenceQueryService.class);

    private final StorageService secondLevelStorage;
    private final StorageService firstLevelStorage;

    public OutboundPersistenceQueryService(Vertx vertx, JsonObject config) {

        String querySecondLevelAddress = config.getString("querySecondLevelAddress");

        firstLevelStorage = new ServiceProxyBuilder(vertx)
                .setAddress(FIRST_LEVEL_STORAGE_SERVICE_ADDRESS)
                .build(StorageService.class);

        secondLevelStorage = new ServiceProxyBuilder(vertx)
                .setAddress(querySecondLevelAddress)
                .build(StorageService.class);
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler) {
        firstLevelStorage.query(queryConfig, uniqueRequestHandle, h -> {
            if (h.succeeded()) {
                LOGGER.info("Query first level storage.");
                resultHandler.handle(Future.succeededFuture());
            } else {
                LOGGER.info("Failed to query first level storage: " + h.cause().getMessage());
                LOGGER.info("Query second level storage.");
                secondLevelStorage.query(queryConfig, uniqueRequestHandle, resultHandler);
            }

        });

    }
}
