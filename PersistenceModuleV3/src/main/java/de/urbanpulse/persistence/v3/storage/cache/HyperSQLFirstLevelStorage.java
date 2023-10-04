package de.urbanpulse.persistence.v3.storage.cache;

import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.jpa.JPAEventEntity;
import de.urbanpulse.persistence.v3.jpa.JPAEventEntityFactory;
import de.urbanpulse.persistence.v3.jpa.JPAWrapperIterator;
import de.urbanpulse.persistence.v3.outbound.BatchSender;
import de.urbanpulse.persistence.v3.storage.AbstractStorage;
import de.urbanpulse.persistence.v3.storage.KeyFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceException;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


import static de.urbanpulse.persistence.v3.storage.KeyFactory.ROW_KEY_PATTERN;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HyperSQLFirstLevelStorage extends AbstractStorage {

    // The second "SID" comparison is necessary otherwise all events that belong to a
    // DIFFERENT SID will be deleted
    static final String DELETE_STATEMENT
            = "DELETE FROM up_events WHERE id NOT IN (SELECT TOP ?1 id FROM up_events WHERE SID = ?2 ORDER BY rowKey DESC) and SID=?2";

    static final String SELECT_SIDS_TO_DELETE_QUERY = "SELECT DISTINCT(sid) FROM up_events";

    /**
     * This is a separate persistence unit from the one used for JPA
     * second-level persistence, also defined in persistence.xml
     */
    private static final String PERSISTENCE_UNIT_NAME = "JPA_event_cache";
    private static final Logger LOGGER = LoggerFactory.getLogger(HyperSQLFirstLevelStorage.class);
    static final AtomicReference<ZonedDateTime> NEXT_CLEANUP_DUE_TIME
            = new AtomicReference<>(ZonedDateTime.now().plusMinutes(1));

    private final KeyFactory keyFactory = new KeyFactory();
    private final JPAEventEntityFactory jpaEntityFactory = new JPAEventEntityFactory();

    protected BatchSender batchSender;
    private EntityManager entityManager;

    private int maxCachedEventsPerSid;

    public HyperSQLFirstLevelStorage(Vertx vertx, JsonObject config) {
        super(vertx, config);

        this.batchSender = new BatchSender(vertx);
    }

    @Override
    public void start(Handler<AsyncResult<Void>> handler) {
        JsonObject defaulFirstLevelConfig = new JsonObject();
        defaulFirstLevelConfig.put("maxCachedEventsPerSid", DEFAULT_MAX_CACHED_EVENTS_PER_SID);
        boolean autoShutdown = defaulFirstLevelConfig.getBoolean("autoShutdown", false);

        JsonObject firstLevelConfig = config.getJsonObject("firstLevelConfig", defaulFirstLevelConfig);
        maxCachedEventsPerSid = firstLevelConfig.getInteger("maxCachedEventsPerSid");

        JsonObject persistenceMapJson = createPersistenceMap(autoShutdown);
        defaulFirstLevelConfig.put("persistenceMap", persistenceMapJson);

        LOGGER.info("creating entity manager for cache...");
        entityManager = createEntityManager(persistenceMapJson);
        LOGGER.info("created manager for cache: " + entityManager);

        handler.handle(Future.succeededFuture());
    }

    private EntityManager createEntityManager(JsonObject persistenceMapJson) {
        Map<String, String> persistenceMap = (Map) persistenceMapJson.getMap();
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, persistenceMap);
        return factory.createEntityManager();
    }

    /**
     *
     * @param autoShutdown if true the database will be shut down after the last
     * connection to it got closed
     * @return an JsonObject representing the persistence map
     */
    private static JsonObject createPersistenceMap(Boolean autoShutdown) {
        JsonObject persistenceMapJson = new JsonObject();
        if (autoShutdown) {
            persistenceMapJson.put("javax.persistence.jdbc.url", "jdbc:hsqldb:mem:eventcache;shutdown=true");
        } else {
            persistenceMapJson.put("javax.persistence.jdbc.url", "jdbc:hsqldb:mem:eventcache;shutdown=false");
        }
        persistenceMapJson.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
        return persistenceMapJson;
    }

    /**
     *
     * @param success If true the transaction is commited, else the transaction
     * is rolled back
     * @param transaction the EntityTransaction object
     */
    private void concludeTransaction(boolean success, EntityTransaction transaction) {
        try {
            if (success) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        } catch (Exception ex) {
            LOGGER.error("failed to conclude JPA transaction for cache", ex);
        }
    }

    @Override
    public void persist(List<JsonObject> events) {
        LOGGER.debug("Starting to persist incoming events");
        boolean success = false;

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        try {
            int countOfPersistedEvents = 0;
            events.forEach(event -> {
                try {
                    JPAEventEntity entity = jpaEntityFactory.create(event);
                    LOGGER.debug("Caching " + entity);
                    Optional<JPAEventEntity> existingEntity = findExistingEntity(entity);
                    LOGGER.trace("Existing entity: " + existingEntity);
                    if (existingEntity.isPresent()) {
                        entityManager.detach(existingEntity.get());
                        entity.setId(existingEntity.get().getId());
                        entityManager.merge(entity);
                    } else {
                        entityManager.persist(entity);
                    }
                    LOGGER.trace("Successfully cached " + entity);
                } catch (RuntimeException e) {
                    LOGGER.error("Unable to cache ", e);
                }
                if (countOfPersistedEvents % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });

            ZonedDateTime now = ZonedDateTime.now();
            if (now.isAfter(NEXT_CLEANUP_DUE_TIME.getAndUpdate((ZonedDateTime oldDue) -> {
                if (now.isAfter(oldDue)) {
                    int minutesSinceOldDue = (int) ((now.toInstant().toEpochMilli() - oldDue.toInstant().toEpochMilli()) / 60000L);
                    ZonedDateTime newDue = oldDue.plusMinutes(minutesSinceOldDue).plusMinutes(1);
                    LOGGER.info("cache cleanup was due [" + oldDue + "], now [" + now + "], next [" + newDue + "]");
                    return newDue;
                } else {
                    return oldDue;
                }
            }))) {
                LOGGER.info("cache cleanup running via [" + this + "]...");
                cleanup();
            }

            success = true;
        } catch (Exception ex) {
            LOGGER.error("error caching events", ex);
        } finally {
            concludeTransaction(success, transaction);
            if (success) {
                LOGGER.debug("...cached batch of [" + events.size() + "] events");
            }
            LOGGER.trace("Finished caching");
        }
    }

    private JPAEventEntity getOldestCachedEventForSid(String sid) {
        TypedQuery<JPAEventEntity> query = entityManager
                .createNamedQuery(JPAEventEntity.QUERY_FOR_SID_OLDEST_FIRST, JPAEventEntity.class).setParameter("sid", sid)
                .setMaxResults(1);
        List<JPAEventEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        }

        JPAEventEntity oldestCachedForSid = results.get(0);
        return oldestCachedForSid;
    }

    private JPAEventEntity getYoungestCachedEventForSid(String sid) {
        TypedQuery<JPAEventEntity> query = entityManager
                .createNamedQuery(JPAEventEntity.QUERY_FOR_SID_YOUNGEST_FIRST, JPAEventEntity.class).setParameter("sid", sid)
                .setMaxResults(1);
        List<JPAEventEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

    private Optional<JPAEventEntity> findExistingEntity(JPAEventEntity entity) {
        Timestamp partitionKey = entity.getPartitionKey();
        Timestamp rowKey = entity.getRowKey();
        String sid = entity.getSid();
        Query rowQuery = entityManager.createNamedQuery(JPAEventEntity.QUERY_ROW_FOR_SID)
                .setParameter("sid", sid)
                .setParameter("rowKey", rowKey)
                .setParameter("partitionKey", partitionKey)
                .setParameter("eventHash", entity.getEventHash());
        //.setMaxResults(1); don't use setMaxResults(1). It slows down this query significantly

        List<JPAEventEntity> results;
        results = rowQuery.getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> queryStartedHandler) {
        if (queryConfig.getSids().isEmpty()) {
            // We don't have to go through all the query stuff if we can instantly return an empty result
            queryStartedHandler.handle(Future.succeededFuture());
            batchSender.sendIteratorResultsInBatches(Collections.emptyIterator(), uniqueRequestHandle);
            return;
        }

        if (queryConfig.getSince() == null && queryConfig.getUntil() == null) {
            List<JPAEventEntity> allPresent = queryConfig
                    .getSids()
                    .stream()
                    .map(this::getYoungestCachedEventForSid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            // We need to check whether we have everything cached. Since that already gives us the complete result, let's just return it.
            if (allPresent.size() == queryConfig.getSids().size()) {
                //Process will start.
                queryStartedHandler.handle(Future.succeededFuture());
                batchSender.sendIteratorResultsInBatches(new JPAWrapperIterator(allPresent), uniqueRequestHandle);
            } else {
                queryStartedHandler.handle(Future.failedFuture(new ServiceException(404, "Range not Cached!")));
            }
            return;
        }

        ZonedDateTime since = ZonedDateTime.from(
                UPDateTimeFormat.getFormatterWithZoneZ().parse(queryConfig.getSince()));
        ZonedDateTime until = ZonedDateTime.from(
                UPDateTimeFormat.getFormatterWithZoneZ().parse(queryConfig.getUntil()));

        boolean allFullyCached = queryConfig.getSids().stream().allMatch(sid -> isRangeFullyCached(sid, since));
        if (!allFullyCached) {
            queryStartedHandler.handle(Future.failedFuture(new ServiceException(404, "Range not Cached!")));
            return;
        }
        //Process will start.
        queryStartedHandler.handle(Future.succeededFuture());

        queryNext(new LinkedList<>(queryConfig.getSids()), uniqueRequestHandle, since, until);
    }

    void queryNext(Queue<String> sidQueue, String uniqueRequestHandle, ZonedDateTime since,
            ZonedDateTime until) {
        String sid = sidQueue.poll();
        Promise<Void> sidPromise = Promise.promise();
        Future<Iterator<JsonObject>> nextFiterator = query(sid, since, until);
        nextFiterator.onComplete(iteratorResult -> {
            if (iteratorResult.succeeded()) {
                batchSender.sendIteratorResultsInBatches(iteratorResult.result(),
                        uniqueRequestHandle, !sidQueue.isEmpty(), sidPromise);
            } else {
                sidPromise.fail(iteratorResult.cause());
            }
        });
        sidPromise.future().onComplete(i -> {
            if (!sidQueue.isEmpty()) {
                queryNext(sidQueue, uniqueRequestHandle, since, until);
            }
        });
    }

    private Future<Iterator<JsonObject>> query(String sid, ZonedDateTime since, ZonedDateTime until) {
        Promise<Iterator<JsonObject>> promise = Promise.promise();

        Iterator<JsonObject> iterator;

        boolean success = false;
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {

            Timestamp sinceAsRowKey = asRowKey(since);
            Timestamp untilAsRowKey = asRowKey(until);

            Query sinceUntilQuery = entityManager.createNamedQuery(JPAEventEntity.QUERY_SINCE_UNTIL_FOR_SID)
                    .setParameter("sid", sid)
                    .setParameter("since", sinceAsRowKey)
                    .setParameter("until", untilAsRowKey);

            List<JPAEventEntity> entities = sinceUntilQuery.getResultList();
            LOGGER.debug("Entities found: " + entities);
            iterator = new JPAWrapperIterator(entities);

            success = true;
            promise.complete(iterator);
        } catch (Exception ex) {
            LOGGER.error("error querying cached events from JPA", ex);
            promise.fail(ex);
        } finally {
            concludeTransaction(success, transaction);
        }

        return promise.future();
    }

    private boolean isRangeFullyCached(String sid, ZonedDateTime since) {
        JPAEventEntity oldestCached = getOldestCachedEventForSid(sid);
        LOGGER.info(Json.encode(oldestCached));
        if (oldestCached == null) {
            return false;
        }

        ZonedDateTime timestampOfOldestCached = ZonedDateTime.ofInstant(Instant.ofEpochMilli(oldestCached.getRowKey().getTime()), ZoneId.of("UTC"));
        return !since.isBefore(timestampOfOldestCached);
    }

    private Timestamp asRowKey(ZonedDateTime dateTime) {
        String rowKey = keyFactory.createRowKey(dateTime);
        return jpaEntityFactory.convertKeyToSQLTimestamp(rowKey, ROW_KEY_PATTERN);
    }

    @Override
    public void stop(Handler<AsyncResult<Void>> handler) {
        LOGGER.info("closing entity manager for cache...");
        try {
            entityManager.close();
            LOGGER.debug("closed entity manager for cache");
            handler.handle(Future.succeededFuture());
        } catch (Exception e) {
            LOGGER.debug("failed to close entity manager for cache");
            handler.handle(Future.failedFuture(e));
        }
    }

    private void cleanup() {
        // This must NOT be called concurrently with cacheIncoming, otherwise there may be constraint violations (#4444).
        // This method does not create a separate transaction anymore but uses the surrounding one.
        boolean success = false;
        int totalDeleteCount = 0;
        try {
            LOGGER.debug("cache cleanup with max [" + maxCachedEventsPerSid + "] per sid...");
            List<String> sids = entityManager.createNativeQuery(SELECT_SIDS_TO_DELETE_QUERY).getResultList();
            for (String sid : sids) {
                int deleteCount = cleanupSid(sid);
                totalDeleteCount += deleteCount;
                if (totalDeleteCount > 0) {
                    LOGGER.info("purged " + deleteCount + " events for sid [" + sid + "] from cache");
                } else {
                    LOGGER.trace("purged no events for sid [" + sid + "] from cache");
                }
            }

            success = true;
        } catch (Exception ex) {
            LOGGER.error("error cleaning cache", ex);
        } finally {
            if (success) {
                if (totalDeleteCount > 0) {
                    LOGGER.info("...cache cleanup done, purged [" + totalDeleteCount + "] total events from cache");
                } else {
                    LOGGER.trace("...cache cleanup done, nothing purged");
                }
            } else {
                LOGGER.warn("Cache cleanup not successful.");
            }
        }
    }

    /**
     * purge all but the most recent N events (via "maxCachedEventsPerSid" field
     * in config JSON) for a given SID from the cache
     *
     * @param sid the sensor's id
     * @return number of purged events
     */
    private int cleanupSid(String sid) {
        return entityManager.createNativeQuery(DELETE_STATEMENT)
                .setParameter(1, maxCachedEventsPerSid)
                .setParameter(2, sid)
                .executeUpdate();
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {
        //Currently no further meters to be registerd. Please add new meters here
    }

    @Override
    protected String getMetricsPrefix() {
        return "up_persistence_firstlevel_hsql";
    }

}
