package de.urbanpulse.persistence.v3.storage;

import de.urbanpulse.outbound.QueryConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class AbstractSecondLevelStorageTest {

    private Vertx vertx;
    private DummySecondLevelStorage dummySecondLevelStorage;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        dummySecondLevelStorage = Mockito.spy(new DummySecondLevelStorage(vertx, new JsonObject()));
    }

    @Test
    public void testSuccessForLatest(TestContext testContext) {
        Async async = testContext.async();
        String uniqueEventBusAddress = "testHandler";
        JsonObject config = new JsonObject().put("sids", new JsonArray().add("1"));
        QueryConfig myQueryConfig = new QueryConfig(config);
        List<JsonObject> dummyObjectList = new ArrayList<>();
        dummyObjectList.add(new JsonObject().put("me", 5));

        Mockito.doReturn(Future.succeededFuture(dummyObjectList.iterator()))
                .when(dummySecondLevelStorage).queryLatest(Mockito.anyString());

        vertx.eventBus().consumer(uniqueEventBusAddress, event -> {
            JsonObject responseObj = (JsonObject) event.body();
            testContext.assertNotNull(responseObj);
            testContext.assertTrue(responseObj.getBoolean("isLast"));
            testContext.assertEquals(5, responseObj.getJsonArray("batch").getJsonObject(0).getInteger("me"));
            async.complete();
        });

        dummySecondLevelStorage.query(myQueryConfig, uniqueEventBusAddress, hndlr -> {
            testContext.assertTrue(hndlr.succeeded());
        });
    }

    @Test
    public void testSuccessSinceUntil(TestContext testContext) {
        Async async = testContext.async();
        String uniqueEventBusAddress = "testHandler";
        JsonObject config = new JsonObject().put("sids", new JsonArray().add("1"))
                .put("since", "2020-12-04T13:36:21.743Z")
                .put("until", "2020-12-07T10:36:21.743Z");
        QueryConfig myQueryConfig = new QueryConfig(config);
        List<JsonObject> dummyObjectList = new ArrayList<>();
        dummyObjectList.add(new JsonObject().put("me", 5));

        Mockito.doReturn(Future.succeededFuture(dummyObjectList.iterator()))
                .when(dummySecondLevelStorage).query(Mockito.anyString(), Mockito.any(), Mockito.any());

        vertx.eventBus().consumer(uniqueEventBusAddress, event -> {
            JsonObject responseObj = (JsonObject) event.body();
            testContext.assertNotNull(responseObj);
            testContext.assertTrue(responseObj.getBoolean("isLast"));
            testContext.assertEquals(5, responseObj.getJsonArray("batch").getJsonObject(0).getInteger("me"));
            async.complete();
        });

        dummySecondLevelStorage.query(myQueryConfig, uniqueEventBusAddress, hndlr -> {
            testContext.assertTrue(hndlr.succeeded());
        });
    }

    @Test
    public void testFailForLatest() {
        String uniqueEventBusAddress = "testHandler";
        JsonObject config = new JsonObject().put("sids", new JsonArray().add("1").add("2"));
        QueryConfig myQueryConfig = new QueryConfig(config);

        Mockito.doReturn(Future.failedFuture("error")).when(dummySecondLevelStorage)
                .queryLatest(Mockito.anyString());

        dummySecondLevelStorage.queryNext(new LinkedList<>(myQueryConfig.getSids()), uniqueEventBusAddress, null, null);
        Mockito.verify(dummySecondLevelStorage, Mockito.times(2)).queryNext(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testEmptySIDsWillReturnEmptyResult(TestContext testContext) {
        Async async = testContext.async(2);
        String uniqueEventBusAddress = "testHandler";
        JsonObject config = new JsonObject().put("sids", new JsonArray());
        QueryConfig myQueryConfig = new QueryConfig(config);

        vertx.eventBus().consumer(uniqueEventBusAddress, event -> {
            JsonObject responseObj = (JsonObject) event.body();
            testContext.assertNotNull(responseObj);
            testContext.assertTrue(responseObj.getBoolean("isLast"));
            testContext.assertTrue(responseObj.getJsonArray("batch").isEmpty());
            async.countDown();
        });

        dummySecondLevelStorage.query(myQueryConfig, uniqueEventBusAddress, hndlr -> {
            testContext.assertTrue(hndlr.succeeded());
            async.countDown();
        });

    }
}
