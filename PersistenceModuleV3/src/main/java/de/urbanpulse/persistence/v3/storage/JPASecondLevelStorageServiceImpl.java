package de.urbanpulse.persistence.v3.storage;

import static de.urbanpulse.persistence.v3.storage.KeyFactory.ROW_KEY_PATTERN;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

import city.ui.shared.db.migrate.exception.MigrationException;
import de.urbanpulse.persistence.v3.jpa.EclipseLinkTableNameSessionCustomizer;
import de.urbanpulse.persistence.v3.jpa.JPAEventEntity;
import de.urbanpulse.persistence.v3.jpa.JPAEventEntityFactory;
import de.urbanpulse.persistence.v3.jpa.JPAWrapperIterator;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * simple implementation for persisting into / querying from JPA
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class JPASecondLevelStorageServiceImpl extends AbstractSecondLevelStorage {

    private static final String PERSISTENCE_UNIT_NAME = "JPA_event_persistence";

    private static final Logger LOGGER = LoggerFactory.getLogger(JPASecondLevelStorageServiceImpl.class);
    private final JPAEventEntityFactory jpaEntityFactory = new JPAEventEntityFactory();
    private final KeyFactory keyFactory = new KeyFactory();

    private EntityManager entityManager;

    public JPASecondLevelStorageServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    public void start(Handler<AsyncResult<Void>> result) {
        JsonObject secondLevelConfig
                = config.getJsonObject("secondLevelConfig", new JsonObject());

        JsonObject persistenceMapJson = secondLevelConfig.getJsonObject("persistenceMap");
        if (null == persistenceMapJson) {
            result.handle(Future.failedFuture("null persistenceMap"));
            return;
        }

        persistenceMapJson.put("eclipselink.session.customizer",
                EclipseLinkTableNameSessionCustomizer.class.getName());

        LOGGER.info("creating entity manager...");
        Map<String, String> persistenceMap = (Map) persistenceMapJson.getMap();
        EntityManagerFactory factory
                = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, persistenceMap);
        entityManager = factory.createEntityManager();
        LOGGER.info("created manager: " + entityManager);
        try {
            migrate();
        } catch (MigrationException e) {
            result.handle(Future.failedFuture(e));
            return;
        }

        result.handle(Future.succeededFuture());
    }

    @Override
    public void stop(Handler<AsyncResult<Void>> result) {
        LOGGER.info("closing entity manager...");
        try {
            entityManager.close();
            LOGGER.info("closed entity manager");
            result.handle(Future.succeededFuture());
        } catch (Exception e) {
            LOGGER.info("failed to close entity manager");
            result.handle(Future.failedFuture(e));
        }
    }

    @Override
    public void persist(List<JsonObject> events) {
        boolean success = false;
        EntityTransaction transaction = entityManager.getTransaction();

        transaction.begin();
        AtomicInteger countOfPersistedEvents = new AtomicInteger(0);
        try {
            events.stream().forEach(event -> {
                try {
                    JPAEventEntity entity = jpaEntityFactory.create(event);
                    JPAEventEntity existingEntity = findExistingEntity(entity);
                    if (existingEntity != null) {
                        entityManager.detach(existingEntity);
                        entity.setId(existingEntity.getId());
                        entityManager.merge(entity);
                    } else {
                        entityManager.persist(entity);
                    }
                    countOfPersistedEvents.incrementAndGet();
                } catch (Exception e) {
                    LOGGER.error("Unable to persist ", e);
                }
                if (countOfPersistedEvents.get() % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }

            });
            incTotalEventsPersistedCounter(events.size());
            success = true;
        } catch (Exception ex) {
            LOGGER.error("error persisting to JPA", ex);
        } finally {
            concludeTransaction(success, transaction);
        }
    }

    private JPAEventEntity findExistingEntity(JPAEventEntity entity) {
        Timestamp partitionKey = entity.getPartitionKey();
        Timestamp rowKey = entity.getRowKey();
        String sid = entity.getSid();
        Query rowQuery = entityManager.createNamedQuery(JPAEventEntity.QUERY_ROW_FOR_SID)
                .setParameter("sid", sid)
                .setParameter("rowKey", rowKey)
                .setParameter("partitionKey", partitionKey)
                .setParameter("eventHash", entity.getEventHash());

        List<JPAEventEntity> results = rowQuery.getResultList();
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    private void concludeTransaction(boolean success, EntityTransaction transaction) {
        try {
            if (success) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        } catch (Exception ex) {
            LOGGER.error("failed to conclude JPA transaction", ex);
        }
    }

    private Future<Iterator<JsonObject>> querySQL(Query query) {
        LOGGER.info("Querying {0}", query);
        Promise<Iterator<JsonObject>> promise = Promise.promise();

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            List<JPAEventEntity> entities = query.getResultList();
            LOGGER.info("Result list has size " + entities.size());
            Iterator<JsonObject> iterator = new JPAWrapperIterator(entities);
            concludeTransaction(true, transaction);
            promise.complete(iterator);
        } catch (Exception ex) {
            LOGGER.error("error querying from JPA", ex);
            concludeTransaction(false, transaction);
            promise.fail(ex);
        }

        return promise.future();
    }

    @Override
    protected Future<Iterator<JsonObject>> query(String sid, ZonedDateTime since,
            ZonedDateTime until) {
        Query sinceUntilQuery
                = entityManager.createNamedQuery(JPAEventEntity.QUERY_SINCE_UNTIL_FOR_SID)
                        .setParameter("sid", sid).setParameter("since", asRowKey(since))
                        .setParameter("until", asRowKey(until));

        return querySQL(sinceUntilQuery);
    }

    @Override
    protected Future<Iterator<JsonObject>> queryLatest(String sid) {
        Query youngestEventForSidQuery
                = entityManager.createNamedQuery(JPAEventEntity.QUERY_FOR_SID_YOUNGEST_FIRST)
                        .setParameter("sid", sid).setMaxResults(1);
        return querySQL(youngestEventForSidQuery);
    }

    private Timestamp asRowKey(ZonedDateTime dateTime) {
        String rowKey = keyFactory.createRowKey(dateTime);
        return jpaEntityFactory.convertKeyToSQLTimestamp(rowKey, ROW_KEY_PATTERN);
    }

    private void migrate() {
        JPAMigrationBean migrationBean = new JPAMigrationBean(entityManager);
        migrationBean.doMigrate();
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {
        //Currently no further meters to be registerd. Please add new meters here
    }

    @Override
    protected String getMetricsPrefix() {
        return "up_persistence_sql";
    }

}
