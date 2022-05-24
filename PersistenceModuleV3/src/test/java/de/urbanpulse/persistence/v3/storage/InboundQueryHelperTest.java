package de.urbanpulse.persistence.v3.storage;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class InboundQueryHelperTest {
    private Vertx vertx;
    private InboundQueryHelper inboundQueryHelper;

    @Before
    public void setUp(){
        vertx = Vertx.vertx();
        inboundQueryHelper = new InboundQueryHelper(vertx, new JsonObject());
    }

    @Test
    public void testPullingSuccessful(TestContext testContext){
        Async async = testContext.async(2);

        vertx.eventBus().localConsumer("pullAddress", hndlr -> {
           hndlr.reply(new JsonArray().add(new JsonObject().put("SID", "7").put("t", "m")));
           async.countDown();
        });

        inboundQueryHelper.startPulling(customHandler -> {
            testContext.assertNotNull(customHandler);
            testContext.assertEquals(1, customHandler.size());
            testContext.assertEquals("7", customHandler.get(0).getString("SID"));
        });
    }

    @Test
    public void testPullingIncorrectMessageBody(TestContext testContext){
        Async async = testContext.async(2);

        vertx.eventBus().localConsumer("pullAddress", hndlr -> {
            hndlr.reply(new JsonObject().put("SID", "7").put("t", "m"));
            async.countDown();
        });

        inboundQueryHelper.startPulling(testContext::assertNull);
    }

}
