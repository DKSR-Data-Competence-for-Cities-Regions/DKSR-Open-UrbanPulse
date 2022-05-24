package de.urbanpulse.persistence.v3.storage.cache;

import io.vertx.core.Vertx;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ServiceException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.mockito.MockitoAnnotations;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class NullFirstLevelStorageTest {

    private NullFirstLevelStorage nullFirstLevelStorage;
    private Vertx vertx;

    @Mock
    JsonObject config;

    public NullFirstLevelStorageTest() {

    }

    @Before
    public void init() {
        vertx = spy(Vertx.vertx());
        MockitoAnnotations.initMocks(this);
        nullFirstLevelStorage = new NullFirstLevelStorage(vertx, config);
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    /**
     * Test of cacheIncoming method, of class NullFirstLevelStorage. The object
     * shouldn't change as it is expected to do nothing.
     *
     * @param context test context
     */
    @Test
    public void testCacheIncoming(TestContext context) {
        System.out.println("cacheIncoming");
        List<JsonObject> list = new ArrayList<>();
        list.add(new JsonObject().put("sid", "123"));
        nullFirstLevelStorage.persist(list);
        verifyZeroInteractions(config);
        verifyZeroInteractions(vertx);
    }

    /**
     * Test of query method, of class NullFirstLevelStorage. Succeeds if result
     * is 'null'
     *
     * @param context
     */
    @Test
    public void testQuery(TestContext context) {

        nullFirstLevelStorage.query(null, null, h -> {
            if (h.succeeded()) {
                context.fail("Exception expected");
            } else {
                context.assertTrue(h.cause() instanceof ServiceException);
            }
        });

    }

    /**
     * Test of start method, of class NullFirstLevelStorage. Should succeed
     * always.
     *
     * @param context
     */
    @Test
    public void testStart(TestContext context) {
        System.out.println("start");
        nullFirstLevelStorage.start((result) -> context.assertTrue(result.succeeded()));
    }

    /**
     * Test of stop method, of class NullFirstLevelStorage.Should succeed
     * always.
     *
     * @param context
     */
    @Test
    public void testStop(TestContext context) {
        System.out.println("stop");
        nullFirstLevelStorage.stop((result) -> context.assertTrue(result.succeeded()));
    }

}
