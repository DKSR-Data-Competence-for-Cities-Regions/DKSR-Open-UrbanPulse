package de.urbanpulse.persistence.v3.storage;


import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Collections;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class JPASecondLevelStorageServiceImplTest {

    private Vertx vertx;
    private JPASecondLevelStorageServiceImpl storage;
    private JsonObject config;

    public JPASecondLevelStorageServiceImplTest() {
    }

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        Async async = context.async();
        config = new JsonObject();
        JsonObject secondLevelConfig = new JsonObject();
        final JsonObject persistenceMap = createPersistenceMap();
        secondLevelConfig.put("persistenceMap", persistenceMap);
        config.put("secondLevelConfig", secondLevelConfig);
        config.put("querySecondLevelAddress", "testQuerySecondLevelAddress");

        Promise<Void> startPromise = Promise.promise();
        storage = new JPASecondLevelStorageServiceImpl(vertx, config);
        storage.start(startPromise);

        startPromise.future().onComplete(result -> {
            context.assertTrue(result.succeeded());
            async.complete();
        });
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        storage.stop(i -> {
            vertx.close(ig -> async.complete());
        });
    }

    /**
     * This test will insert some invalid data and check that it didn't get added
     *
     * @param context
     */
    @Test
    public void testPersistInvalidObjects(TestContext context) {
        // cache some invalid events. expect that none of them gets cached
        for (JsonObject event : PredefinedEvents.invalidObjects) {
            try {
                storage.persist(Collections.singletonList(event));
            } catch (NullPointerException e) {
            } catch (Exception e) {
            }
        }
        // assert that no invalid event got cached
        Async async = context.async(PredefinedEvents.invalidObjects.length);
        Stream.of(PredefinedEvents.invalidObjects).forEach(event -> {
            String sid;
            try {
                sid = event.getString("SID");
            } catch (Exception e) {
                async.countDown();
                return;
            }
            final Future<Iterator<JsonObject>> queryResult =
                    storage.query(sid, PredefinedEvents.time, ZonedDateTime.now());
            queryResult.onFailure(context::fail).onSuccess(iterator -> {
                final boolean hasNext = iterator.hasNext();
                JsonObject nextObject = hasNext ? iterator.next() : new JsonObject();
                context.assertFalse(hasNext,
                        "the storage is caching invalid events:\n" + nextObject.encodePrettily());
                async.countDown();
            });
        });
    }

    /**
     * This test will insert some valid data with duplicates and check that it got added. It is
     * expected that only one of the duplicates (same SID and timestamp) gets added
     *
     * @param context
     */
    @Test
    public void testPersistValidObjects(TestContext context) {

        // assert that all non-duplicate events got cached
        storage.persist(Arrays.asList(PredefinedEvents.validObjects));

        // minus 1 because two of the validObjects are identical and only get cached once
        Async async = context.async(PredefinedEvents.validObjects.length - 1);
        // just interested in SID for query
        Stream.of(PredefinedEvents.validObjects).map(event -> event.getString("SID"))
                // just interested in unique SIDs
                .distinct().forEach(sid -> {
                    final Future<Iterator<JsonObject>> queryResult = storage.query(sid,
                            PredefinedEvents.time, ZonedDateTime.now().plusSeconds(2));
                    queryResult.onFailure(context::fail).onSuccess(iterator -> {
                        context.assertTrue(iterator.hasNext());
                        iterator.forEachRemaining(i -> async.countDown());
                    });
                });
    }

    @Test
    public void testQueryLatest(TestContext context) {
        // First persisting some valid events
        storage.persist(Arrays.asList(PredefinedEvents.validObjects));

        // Only two events from different sid and we have the latest
        Async async = context.async(2);
        Stream.of(PredefinedEvents.validObjects).map(event -> event.getString("SID")).distinct()
                .forEach(sid -> {
                    final Future<Iterator<JsonObject>> queryResult = storage.queryLatest(sid);
                    queryResult.onFailure(context::fail).onSuccess(iterator -> {
                        context.assertTrue(iterator.hasNext());
                        iterator.forEachRemaining(i -> async.countDown());
                    });
                });
    }

    @Test
    public void testNullPersistenceMapWillFailStart(TestContext context) {
        Async async = context.async();
        config.getJsonObject("secondLevelConfig").remove("persistenceMap");
        Promise<Void> startPromise = Promise.promise();
        storage = new JPASecondLevelStorageServiceImpl(vertx, config);
        storage.start(startPromise);

        startPromise.future().onComplete(result -> {
            context.assertTrue(result.failed());
            async.complete();
        });
    }

    private JsonObject createPersistenceMap() {
        JsonObject persistenceMapJson = new JsonObject();
        String dbName = RandomStringUtils.randomAlphanumeric(10);
        persistenceMapJson.put("javax.persistence.jdbc.url",
                "jdbc:hsqldb:mem:" + dbName + ";shutdown=true");
        persistenceMapJson.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
        return persistenceMapJson;
    }

}
