package de.urbanpulse.persistence.v3.storage.cache;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import io.vertx.core.Promise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.storage.StorageService;
import de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle;
import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_ADDRESS_PROPERTY;
import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_CLASS_PROPERTY;
import static de.urbanpulse.persistence.v3.storage.cache.FirstLevelStorageConst.FIRST_LEVEL_STORAGE_SERVICE_ADDRESS;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class HyperSQLFirstLevelStorageVertxUnitTest {

    private static final String SID = "6581";

    private Vertx vertx;

    @Before
    public void init(TestContext context) {
        MockitoAnnotations.initMocks(this);
        vertx = Vertx.vertx();
        JsonObject config = new JsonObject();
        config.put(SERVICE_CLASS_PROPERTY, HyperSQLFirstLevelStorage.class.getName());
        config.put(SERVICE_ADDRESS_PROPERTY, FIRST_LEVEL_STORAGE_SERVICE_ADDRESS);
        config.put("firstLevelConfig", new JsonObject().put("maxCachedEventsPerSid", 2));
        config.put("autoShutdown", true);
        vertx.deployVerticle(StorageServiceProviderVerticle.class.getName(), new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());

    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    /**
     * This test requires to be run in UTC timezone (as does the whole PersistenceModuleV3)
     * <p>
     * you can enforce the timzeone for unit tests in pom.xml's build/plugins section like this:
     *
     * <pre>
     * &lt;plugin&gt;
     *    &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
     *    &lt;artifactId&gt;maven-surefire-plugin&lt;/artifactId&gt;
     *    &lt;version&gt;2.19.1&lt;/version&gt;
     *    &lt;configuration&gt;
     *       &lt;argLine&gt;-Duser.timezone=UTC&lt;/argLine&gt;
     *    &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     *
     * @param context
     */
    @Test
    public void testPersist(TestContext context) {
        assertEquals("UTC", System.getProperty("user.timezone"));
        Async async = context.async();

        ZonedDateTime firstEvent = OffsetDateTime.of(2016, 10, 30, 0, 0, 0, 0, ZoneOffset.of("+02:00")).toZonedDateTime();
        ZonedDateTime secondEvent = OffsetDateTime.of(2016, 10, 30, 1, 0, 0, 0, ZoneOffset.of("+02:00")).toZonedDateTime();

        QueryConfig queryConfig = new QueryConfig();
        List<String> sids = new ArrayList<>();
        sids.add(SID);
        queryConfig
                .setSids(sids)
                .setSince(UPDateTimeFormat.getFormatterWithZoneZ().format(firstEvent))
                .setUntil(UPDateTimeFormat.getFormatterWithZoneZ().format(secondEvent));

        List<JsonObject> events = new ArrayList<>(configureAndCreateEvents(firstEvent, secondEvent));

        JsonObject expected = new JsonObject("{\n"
                + "  \"batch\" : [ {\n"
                + "    \"SID\" : \"6581\",\n"
                + "    \"timestamp\" : \"2016-10-29T22:00:00.000Z\"\n"
                + "  }, {\n"
                + "    \"SID\" : \"6581\",\n"
                + "    \"timestamp\" : \"2016-10-29T23:00:00.000Z\"\n"
                + "  } ],\n"
                + "  \"isLast\" : true"
                + "}");

        runTestQuery(events, queryConfig).future()
                .onSuccess(hndlr-> {
                    context.assertEquals(expected, hndlr);
                    async.complete();
                })
                .onFailure(context::fail);
    }

    @Test
    public void testPersistAndQueryLatest(TestContext context) {
        assertEquals("UTC", System.getProperty("user.timezone"));
        Async async = context.async();

        ZonedDateTime firstEvent = OffsetDateTime.of(2016, 10, 30, 0, 0, 0, 0, ZoneOffset.of("+02:00")).toZonedDateTime();
        ZonedDateTime secondEvent = OffsetDateTime.of(2016, 10, 30, 1, 0, 0, 0, ZoneOffset.of("+02:00")).toZonedDateTime();

        QueryConfig queryConfig = new QueryConfig();
        List<String> sids = new ArrayList<>();
        sids.add(SID);
        queryConfig.setSids(sids);

        List<JsonObject> events = new ArrayList<>(configureAndCreateEvents(firstEvent, secondEvent));

        JsonObject expected = new JsonObject("{\n" +
                "    \"batch\": [{\n" +
                "            \"SID\": \"6581\",\n" +
                "            \"timestamp\": \"2016-10-29T23:00:00.000Z\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"isLast\": true\n" +
                "}\n");

        runTestQuery(events, queryConfig).future()
                .onSuccess(hndlr-> {
                    context.assertEquals(expected, hndlr);
                    async.complete();
                })
                .onFailure(context::fail);
    }

    @Test
    public void emptyQueryWillStartSuccessfully(TestContext context) {
        Async async = context.async();
        QueryConfig queryConfig = new QueryConfig().setSids(Collections.emptyList());
        JsonObject expected = new JsonObject("{\n" +
                "    \"batch\": [],\n" +
                "    \"isLast\": true\n" +
                "}\n");
        runTestQuery(Collections.emptyList(), queryConfig).future()
                .onSuccess(hndlr-> {
                    context.assertEquals(expected, hndlr);
                    async.complete();
                })
                .onFailure(context::fail);
    }



    private Promise<JsonObject> runTestQuery(List<JsonObject> events, QueryConfig queryConfig){
        Promise<JsonObject> testResult = Promise.promise();

        vertx.eventBus().<JsonObject>consumer("test", h -> {
            JsonObject result = h.body();
            result.remove("batchTimestamp");
            testResult.complete(result);
        });

        StorageService firstLevelStorage = new ServiceProxyBuilder(vertx)
                .setAddress(FIRST_LEVEL_STORAGE_SERVICE_ADDRESS)
                .build(StorageService.class);

        firstLevelStorage.persist(events);

        firstLevelStorage.query(queryConfig, "test", h -> {
            System.out.print("Query started!");
        });


        return testResult;
    }


    private List<JsonObject> configureAndCreateEvents(ZonedDateTime firstEvent, ZonedDateTime secondEvent){
        List<JsonObject> events = new LinkedList<>();
        JsonObject first = new JsonObject();
        first.put("SID", SID);
        first.put("timestamp", UPDateTimeFormat.getFormatterWithZoneZ().format(firstEvent));
        events.add(first);

        JsonObject second = new JsonObject();
        second.put("SID", SID);
        second.put("timestamp", UPDateTimeFormat.getFormatterWithZoneZ().format(secondEvent));
        events.add(second);

        return events;
    }

}
