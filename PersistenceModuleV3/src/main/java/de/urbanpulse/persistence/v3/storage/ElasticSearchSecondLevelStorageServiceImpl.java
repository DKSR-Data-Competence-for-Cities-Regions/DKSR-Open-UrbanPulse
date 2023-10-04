package de.urbanpulse.persistence.v3.storage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.monitoring.helper.GaugeBuilder;
import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.outbound.BatchSender;
import static de.urbanpulse.persistence.v3.storage.AbstractSecondLevelStorage.BATCH_SIZE;
import de.urbanpulse.persistence.v3.storage.elasticsearch.ElasticSearchReadStream;
import de.urbanpulse.persistence.v3.storage.elasticsearch.clients.ElasticSearchCustomClientBuilder;
import de.urbanpulse.persistence.v3.storage.elasticsearch.clients.ElasticSearchRestHighLevelClientWrapper;
import de.urbanpulse.persistence.v3.storage.elasticsearch.helpers.ElasticSearchSearchAfterHelper;
import de.urbanpulse.persistence.v3.storage.elasticsearch.helpers.ElasticSearchSetupHelper;
import de.urbanpulse.persistence.v3.storage.elasticsearch.helpers.RangedBoolQueryBuilder;
import de.urbanpulse.persistence.v3.storage.elasticsearch.helpers.LatestEventTermQueryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.impl.MessageProducerImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchSecondLevelStorageServiceImpl extends AbstractStorage {

    private static final Logger LOGGER
            = LoggerFactory.getLogger(ElasticSearchSecondLevelStorageServiceImpl.class);

    public static final String METRICS_PREFIX = "up_persistence_elasticsearch";
    
    private static final String TIME_FORMAT_FOR_INDEX = "yyyyMM";
    public static final String EVENT_TYPE_SID_INDEX_SUFFIX = "_index_name_prefix_to_sid_index";
    public static final String UP_HASH_FIELD_NAME = "upHash"; // the field with the hash
    public static final String UP_INDEX_FIELD_NAME = "indexNamePrefix";
    public static final String UP_GEO_LOCATION_FIELD = "esGeoLocation";
    public static final String UP_INTERFIX = "-up-";
    public static final String INNER_HIT_PROPERTY_NAME = "most_recent";

    private static final int MAX_REQUESTS_DEFAULT = 2;
    private static final long PERIODIC_TIMER_DEFAULT = 500;
    private static final int MAX_DOCUMENT_BULK_SIZE = 1000;
    private static final int MAX_CACHED_SID_MAPPINGS_DEFAULT = 10000;

    protected ElasticSearchRestHighLevelClientWrapper client;
    protected ElasticSearchSetupHelper elasticSearchSetupHelper;
    protected Map<String, List<JsonObject>> eventsSimpleQueue;
    protected AtomicInteger currentAmountRunningBulkRequests;
    protected KeyFactory keyFactory;
    protected String password;
    protected String user;
    protected String httpScheme;
    protected String elasticSearchHost;
    protected String upIndexPrefix;
    protected String indexTemplateName;
    protected String mappingIndexName;
    protected boolean verifyHost;
    protected boolean trustAll;
    protected int port;
    protected int maxRequests;
    protected int maxBatchSize;
    protected int maxCachedSIDMappings;

    private final BatchSender batchSender;

    private final AtomicInteger eventsInQueue = new AtomicInteger();

    // Remembers for which SIDs we have already created a mapping during the current run, so we
    // don't have to do it again. Limited to a certain size.
    protected LinkedHashSet<String> cachedSIDMappings;

    public ElasticSearchSecondLevelStorageServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);

        batchSender = new BatchSender(vertx);
        cachedSIDMappings = new LinkedHashSet<>();
        eventsSimpleQueue = new ConcurrentHashMap<>();
        keyFactory = new KeyFactory();
        currentAmountRunningBulkRequests = new AtomicInteger(0);
    }

    private JsonObject config() {
        return this.config.copy();
    }

    @Override
    protected String getMetricsPrefix() {
        return METRICS_PREFIX;
    }

    @Override
    protected void registerAdditionalMeters(MeterRegistry registry) {
        if (registry != null) {
            GaugeBuilder.build(registry, METRICS_PREFIX + "_running_bulk_requests", currentAmountRunningBulkRequests, AtomicInteger::get)
                    .register(registry);   
            GaugeBuilder.build(registry, METRICS_PREFIX + "_events_in_queue", eventsInQueue, AtomicInteger::get)
                    .register(registry);
        }
    }

    @Override
    public void start(Handler<AsyncResult<Void>> result) {
        LOGGER.info("Starting ElasticSearch client...");
        JsonObject secondLevelConfig
                = config().getJsonObject("secondLevelConfig", new JsonObject());
        elasticSearchHost = secondLevelConfig.getString("elasticSearchHost", "localhost");
        port = secondLevelConfig.getInteger("elasticSearchPort", 9200);
        // local or testing instances might not have https
        httpScheme = secondLevelConfig.getString("elasticSearchHttpScheme", "http");
        user = secondLevelConfig.getString("user");
        password = secondLevelConfig.getString("password");
        // these are relevant for a local or a testing instance that does not have a valid
        // certificate
        verifyHost = secondLevelConfig.getBoolean("verifyHost", true);
        trustAll = secondLevelConfig.getBoolean("trustAll", false);
        maxRequests = secondLevelConfig.getInteger("maxRequests", MAX_REQUESTS_DEFAULT);
        long periodicSendTimer
                = secondLevelConfig.getLong("periodicSendTimer", PERIODIC_TIMER_DEFAULT);
        String tenant = secondLevelConfig.getString("elasticSearchTenant", "default");
        upIndexPrefix = tenant + UP_INTERFIX;
        indexTemplateName = tenant + "-index-template";
        mappingIndexName = tenant + EVENT_TYPE_SID_INDEX_SUFFIX;

        maxBatchSize = secondLevelConfig.getInteger("maxBatchSize", 1000);
        maxCachedSIDMappings = secondLevelConfig.getInteger("maxCachedSIDMappings", MAX_CACHED_SID_MAPPINGS_DEFAULT);

        initElasticSearchHighLevelClient();

        elasticSearchSetupHelper = new ElasticSearchSetupHelper(client, mappingIndexName,
                indexTemplateName, upIndexPrefix);

        setupElasticSearch().onComplete(hndlr -> {
            if (hndlr.failed()) {
                LOGGER.error(hndlr.cause());
                result.handle(Future.failedFuture(hndlr.cause()));
            } else {
                try {
                    vertx.setPeriodic(periodicSendTimer, this::persistBatchedWithTimer);
                    new InboundQueryHelper(vertx, config()).startPulling(this::persist);
                    result.handle(Future.succeededFuture());
                } catch (Exception e) {
                    LOGGER.error("Failed starting verticle {0}",
                            ElasticSearchSecondLevelStorageServiceImpl.class.getName(), e);
                }
            }
        });
    }

    // this way we can mock this and have it do our biding
    protected Future<CompositeFuture> setupElasticSearch() {
        return CompositeFuture.all(
                elasticSearchSetupHelper
                        .checkForAndCreateEventTypeSIDIndex(vertx.getOrCreateContext()),
                elasticSearchSetupHelper
                        .checkForAndCreateIndexTemplate(vertx.getOrCreateContext()));
    }

    protected void closeElasticSearchClient() throws IOException {
        if (client != null && client.getClient() != null) {
            client.close();
        }
    }

    protected void initElasticSearchHighLevelClient() {
        try {
            closeElasticSearchClient();
            client = new ElasticSearchCustomClientBuilder(user, password, httpScheme,
                    elasticSearchHost, port, trustAll, verifyHost).build();
        } catch (Exception e) {
            LOGGER.error("Was not able to create ElasticSearch Client!");
        }
    }

    @Override
    public void stop(Handler<AsyncResult<Void>> result) {
        try {
            closeElasticSearchClient();
            result.handle(Future.succeededFuture());
        } catch (IOException ex) {
            result.handle(Future.failedFuture(ex));
        }

    }

    @Override
    public void persist(List<JsonObject> events) {
        LOGGER.debug("Persist events amount: " + events.size());

        if (!events.isEmpty()) {
            events.stream()
                    .forEach(this::handleIndexCreationAndDocumentSavingForEvent);
        }
    }

    private void handleIndexCreationAndDocumentSavingForEvent(JsonObject event) {
        String eventType = extractEventTypeFromEvent(event);
        if (eventType != null && event.containsKey("timestamp")) {
            String indexNameForEventType = createIndexNameForEventType(eventType, event.getString("timestamp"));
            List<JsonObject> eventsInQueue = eventsSimpleQueue
                    .computeIfAbsent(indexNameForEventType, func -> new ArrayList<>());
            eventsInQueue.add(event);
        } else {
            LOGGER.warn("Received event missing fields! {0}",
                    event.encode());
        }
    }

    private int getQueueSize() {
        int eventsInQ = eventsSimpleQueue.keySet().stream()
                .mapToInt(key -> eventsSimpleQueue.get(key).size()).sum();
        eventsInQueue.set(eventsInQ);

        return eventsInQ;
    }

    private void persistBatchedWithTimer(Long timerId) {
        if (currentAmountRunningBulkRequests.get() >= maxRequests) {
            return;
        }

        final Context vertxContext = vertx.getOrCreateContext();
        int eventsAmount = getQueueSize();
        LOGGER.debug("+++++ Map EventsSize Before: " + eventsAmount);
        if (eventsAmount > MAX_DOCUMENT_BULK_SIZE || (currentAmountRunningBulkRequests.get() == 0 && eventsAmount > 0)) {
            saveBulkDocuments(vertxContext);
        }
        LOGGER.debug("+++++ Map EventsSize After: " + eventsSimpleQueue.keySet().stream()
                .mapToInt(key -> eventsSimpleQueue.get(key).size()).sum());
    }

    private String extractEventTypeFromEvent(JsonObject event) {
        return event.getJsonObject("_headers", new JsonObject()).getString("eventType");
    }

    // index name will be [tenant]-[up]-[eventtype]-[YYYYMM]
    protected String createIndexNameForEventType(String eventType, String timestamp) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(TIME_FORMAT_FOR_INDEX);
        ZonedDateTime localDate = ZonedDateTime.parse(timestamp, UPDateTimeFormat.getFormatterWithZoneZ());
        return upIndexPrefix + eventType.toLowerCase() + "-" + dtf.format(localDate);
    }

    protected void saveBulkDocuments(Context context) {
        BulkRequest bulkRequest = createBulkRequest();
        UUID counterId = UUID.randomUUID();
        LOGGER.debug("Starting with the bulkAsync insert at: " + ZonedDateTime.now() + " with ID: "
                + counterId);
        LOGGER.debug("Incrementing request counter to: "
                + currentAmountRunningBulkRequests.incrementAndGet());

        Optional<Timer.Sample> optionalTimer = registry
                .map(reg -> Optional.of(Timer.start(reg)))
                .orElse(Optional.empty());

        client.bulkAsync(bulkRequest, context).onComplete(bulkHndlr -> {
            currentAmountRunningBulkRequests.decrementAndGet();
            if (bulkHndlr.failed()) {
                LOGGER.warn("Could not bulkImport Documents!", bulkHndlr.cause());
                handleOnFailure(new Exception(bulkHndlr.cause()));
            } else {
                LOGGER.debug("Finished with the bulkAsync insert at: " + ZonedDateTime.now()
                        + " with ID: " + counterId);

                if (registry.isPresent()) {
                    optionalTimer.ifPresent(timer -> timer.stop(registry.get().timer("persistence_persist_duration")));
                }
                double numberOfDocuments = bulkRequest.numberOfActions();
                incTotalEventsPersistedCounter(numberOfDocuments);

                context.runOnContext(hndlr -> persistBatchedWithTimer(-1L));
            }
        });

        // We may have more to do if current batch was not enough. Immediately take action (if we have too many requests already, this will do nothing).
        persistBatchedWithTimer(-1L);
    }

    private BulkRequest createBulkRequest() {
        // Pending: Ideally, we would not leave it to elasticsearch to guess the mapping correctly,
        // but instead create it explicitly. That would give us the option to only
        // index the fields that we actually need, resulting in a performance boost.
        // That's not trivial though, we would probably have to evaluate the event type
        // and add some meta-information there.

        BulkRequest bulkRequest = new BulkRequest();
        Set<String> keySet = eventsSimpleQueue.keySet();

        for (String key : keySet) {
            List<JsonObject> eventsList = eventsSimpleQueue.getOrDefault(key, Collections.emptyList());
            for (Iterator<JsonObject> iterator = eventsList.iterator(); iterator.hasNext();) {
                JsonObject jsonObject = iterator.next();

                IndexRequest indexRequest = createIndexRequest(jsonObject, key);
                bulkRequest.add(indexRequest);

                // Only add the mapping if we haven't mapped this SID before in the current run
                String sid = jsonObject.getString("SID");
                if (!isSIDMappingCached(sid)) {
                    bulkRequest.add(createUpsertRequestForSIDToEventTypeMapping(jsonObject));
                }

                iterator.remove();
                if (bulkRequest.numberOfActions() >= maxBatchSize) {
                    return bulkRequest;
                }
            }
            if (eventsList.isEmpty()) {
                eventsSimpleQueue.remove(key);
            }
        }

        return bulkRequest;
    }

    private IndexRequest createIndexRequest(JsonObject jsonObject, String key) {
        addHashToTheEvent(jsonObject);
        addUpGeoLocationToEventIfNecessary(jsonObject);
        String id = createIdForEvent(jsonObject);
        return new IndexRequest(key).id(id).source(jsonObject.encode(), XContentType.JSON);
    }

    // we don't want to have duplicates in the index
    protected String createIdForEvent(JsonObject event) {
        return event.getString("SID") + "_" + keyFactory.createRowKey(event);
    }

    protected JsonObject addUpGeoLocationToEventIfNecessary(JsonObject event) {
        if (event.containsKey("lat") && event.containsKey("lon")) {
            event.put(UP_GEO_LOCATION_FIELD,
                    new JsonArray().add(event.getDouble("lon")).add(event.getDouble("lat")));
        }

        return event;
    }

    // will be used as a second parameter for the searchAfter query
    protected JsonObject addHashToTheEvent(JsonObject event) {
        return event.put(UP_HASH_FIELD_NAME, keyFactory.createRowKey(event));
    }

    // this will write to the mapping index
    protected UpdateRequest createUpsertRequestForSIDToEventTypeMapping(JsonObject jsonDoc) {
        String indexName = upIndexPrefix + extractEventTypeFromEvent(jsonDoc).toLowerCase();
        String sid = jsonDoc.getString("SID");
        String docId = indexName + "/" + sid;
        JsonObject sidToEventTypeJson
                = new JsonObject().put(UP_INDEX_FIELD_NAME, indexName).put("SID", sid);

        IndexRequest insertDocument = new IndexRequest(mappingIndexName).id(docId)
                .source(sidToEventTypeJson.encode(), XContentType.JSON);
        UpdateRequest upsertRequest = new UpdateRequest(mappingIndexName, docId)
                .doc(sidToEventTypeJson.encode(), XContentType.JSON).upsert(insertDocument);
        upsertRequest.id(docId);

        cacheMappedSID(sid);

        return upsertRequest;
    }

    protected void querySIDForEventTypeHighLevelClient(List<String> sids, ZonedDateTime since,
            ZonedDateTime until, String uniqueRequestHandle, int batchSize, Context context) {

        SearchRequest searchRequest = new SearchRequest(mappingIndexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // Pending: By getting the first SID, we only support lists of SIDs that belong to the same
        // event type. If we want to support multiple event types, this needs to be changed
        searchSourceBuilder.query(QueryBuilders.matchQuery("SID", sids.get(0)));
        searchRequest.source(searchSourceBuilder);

        client.searchForEventTypeAsync(searchRequest, context).onComplete(indexNameHndlr -> {
            if (indexNameHndlr.succeeded()) {
                if (since != null && until != null) {
                    queryDocumentCountHighLevel(indexNameHndlr.result(), sids, since, until,
                            batchSize, uniqueRequestHandle, context);
                } else {
                    queryLastDocumentForSids(indexNameHndlr.result(), sids, uniqueRequestHandle,
                            context);
                }
            } else {
                LOGGER.warn("No IndexName found for SID {0}", sids);
                sendLastMessageWhenNoDocumentsFound(uniqueRequestHandle);
                handleOnFailure(new Exception(indexNameHndlr.cause()));
            }
        });
    }

    protected void queryLastDocumentForSids(String indexName, List<String> sids,
            String uniqueRequestHandle, Context context) {
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(sids.size(), true, sids);

        LOGGER.debug("Starting to query last document for sid: {0} ", sids);

        List<String> batch = new ArrayList<>();
        queue.drainTo(batch, BATCH_SIZE);

        client.searchLatestDocumentsAsync(createSearchRequest(indexName, batch), context)
                .onComplete(res
                        -> handleSearchResult(res, uniqueRequestHandle, queue.isEmpty())
                        .onComplete(handler -> {
                            if (!queue.isEmpty()) {
                                queryLastDocumentForSids(indexName, new ArrayList<>(queue),
                                        uniqueRequestHandle, context);
                            }
                        }));

    }

    private SearchRequest createSearchRequest(String indexName, List<String> sids) {
        SearchRequest searchRequest = new SearchRequest(indexName + "-*");
        SearchSourceBuilder searchSourceBuilder
                = LatestEventTermQueryBuilder.createTermQueryBuilder(sids, INNER_HIT_PROPERTY_NAME);

        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    private Future<Void> handleSearchResult(AsyncResult<List<JsonObject>> searchLastDocumentHandler, String uniqueRequestHandle, boolean lastBatch) {
        Promise<Void> promise = Promise.promise();
        if (searchLastDocumentHandler.succeeded()) {
            LOGGER.debug("Search last document request succeeded!");
            if (searchLastDocumentHandler.result() != null) {
                List<JsonObject> batchData = searchLastDocumentHandler.result();

                batchSender.sendIteratorResultsInBatches(batchData.iterator(),
                        uniqueRequestHandle, !lastBatch, promise);
            } else {
                sendLastMessageWhenNoDocumentsFound(uniqueRequestHandle);
                promise.complete();
            }
        } else {
            LOGGER.error("Unable to get last document! ",
                    searchLastDocumentHandler.cause());
            sendLastMessageWhenNoDocumentsFound(uniqueRequestHandle);
            handleOnFailure(new Exception(searchLastDocumentHandler.cause()));
            promise.fail(searchLastDocumentHandler.cause());
        }
        return promise.future();
    }

    /*
     * This library sets a default size for a get docs request to be 10. To be able to query all of
     * the documents that will be the result to this query we will first have to take their count
     * and set the SearchSourceBuilder for the query to have a size equal to the count of found
     * documents.
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-count.
     * html
     */
    protected void queryDocumentCountHighLevel(String indexName, List<String> sids, ZonedDateTime since,
            ZonedDateTime until, int batchSize, String uniqueRequestHandle, Context context) {
        CountRequest countRequest = new CountRequest(indexName + "-*");
        countRequest
                .query(RangedBoolQueryBuilder.createRangedBooleanQuery(since, until, sids).query());

        client.countAsync(countRequest, context).onComplete(countHndlr -> {
            if (countHndlr.succeeded()) {
                if (countHndlr.result() > 0) { // do we have any documents for the query
                    SearchRequest searchRequest = new SearchRequest(indexName + "-*");

                    LOGGER.info("Amount of documents to get: " + countHndlr.result());

                    ElasticSearchSearchAfterHelper elasticSearchSearchAfterHighLevelClient
                            = new ElasticSearchSearchAfterHelper(client, searchRequest, sids, since,
                                    until, context);

                    ElasticSearchReadStream readStream = new ElasticSearchReadStream(batchSize,
                            elasticSearchSearchAfterHighLevelClient, countHndlr.result(),
                            since.toEpochSecond());

                    Pump.pump(readStream, new MessageProducerImpl<>(vertx,
                            uniqueRequestHandle, true, new DeliveryOptions())).start();
                } else {
                    sendLastMessageWhenNoDocumentsFound(uniqueRequestHandle);
                }
            } else {
                LOGGER.error("Unable to get documents count! ", countHndlr.cause());
                sendLastMessageWhenNoDocumentsFound(uniqueRequestHandle);
                handleOnFailure(new Exception(countHndlr.cause()));
            }
        });
    }

    // https://github.com/elastic/elasticsearch/issues/49124
    // https://github.com/elastic/elasticsearch/issues/39946
    // there are some issues with the client not being able to write data so when we get an
    // IllegalStateException
    // that has a specific message in it, we will try to recreate the client in this case.
    protected void handleOnFailure(Exception e) {
        if (e instanceof IllegalStateException
                && e.getMessage().toLowerCase().contains("i/o reactor status: stopped")) {
            LOGGER.warn(
                    "IllegalStateException the client was stopped! Will recreate the client...");
            initElasticSearchHighLevelClient();
        }
    }

    protected void sendLastMessageWhenNoDocumentsFound(String uniqueRequestHandle) {
        JsonObject sensorData = new JsonObject();

        sensorData.put("batch", new JsonArray());
        sensorData.put("isLast", true);
        sensorData.put("batchTimestamp", LocalDateTime.now().toString());
        vertx.eventBus().send(uniqueRequestHandle, sensorData);
    }

    @Override
    public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler) {

        final Context context = vertx.getOrCreateContext();

        LOGGER.info("############ received request at {0} for handler {1}", LocalDateTime.now(),
                uniqueRequestHandle);
        if (queryConfig.getSince() == null && queryConfig.getUntil() == null) {
            querySIDForEventTypeHighLevelClient(queryConfig.getSids(), null, null, uniqueRequestHandle, 1, context);
        } else {
            ZonedDateTime since = ZonedDateTime.parse(queryConfig.getSince(),
                    UPDateTimeFormat.getFormatterWithZoneZ());
            ZonedDateTime until = ZonedDateTime.parse(queryConfig.getUntil(),
                    UPDateTimeFormat.getFormatterWithZoneZ());
            querySIDForEventTypeHighLevelClient(queryConfig.getSids(), since, until, uniqueRequestHandle, BATCH_SIZE,
                    context);
        }
        resultHandler.handle(Future.succeededFuture());
    }

    protected void cacheMappedSID(String sid) {
        if (maxCachedSIDMappings > 0) {
            cachedSIDMappings.add(sid);
            if (cachedSIDMappings.size() > maxCachedSIDMappings) {
                Iterator<String> iterator = cachedSIDMappings.iterator();
                while (cachedSIDMappings.size() > maxCachedSIDMappings && iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

    protected boolean isSIDMappingCached(String sid) {
        return cachedSIDMappings.contains(sid);
    }
}
