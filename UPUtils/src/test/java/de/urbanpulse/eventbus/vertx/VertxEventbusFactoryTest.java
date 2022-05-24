package de.urbanpulse.eventbus.vertx;

import io.vertx.core.Vertx;
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
public class VertxEventbusFactoryTest {
    private VertxEventbusFactory vertxEventbusFactory;
    private static final String address = "test-addr";

    @Before
    public void setUp() {
        vertxEventbusFactory = new VertxEventbusFactory(Vertx.vertx(), new JsonObject());
    }

    @Test
    public void createMessageConsumer(TestContext context) {
        Async async = context.async();

        vertxEventbusFactory.createMessageConsumer(new JsonObject().put("address", address), hndlr -> {
            context.assertTrue(hndlr.succeeded());
            context.assertNotNull(hndlr.result());
            async.complete();
        });
    }

    @Test
    public void createMessageProducer(TestContext context) {
        Async async = context.async();

        vertxEventbusFactory.createMessageProducer(hndlr -> {
            context.assertTrue(hndlr.succeeded());
            context.assertNotNull(hndlr.result());
            async.complete();
        });
    }

    @Test
    public void createMessageConsumer_nullConfig(TestContext context){
        Async async = context.async();

        vertxEventbusFactory.createMessageConsumer(new JsonObject(), hndlr -> {
            context.assertTrue(hndlr.failed());
            context.assertNull(hndlr.result());
            async.complete();
        });
    }
}
