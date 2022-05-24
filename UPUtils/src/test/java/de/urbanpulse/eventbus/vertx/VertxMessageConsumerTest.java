package de.urbanpulse.eventbus.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class VertxMessageConsumerTest {
    private static final String address = "test.consumer.address";

    private Vertx vertx;
    private VertxMessageConsumer vertxMessageConsumer;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        vertxMessageConsumer = new VertxMessageConsumer(vertx, new JsonObject().put("address", address));
    }

    @Test
    public void handleEvent(TestContext context) {
        Async async = context.async();
        JsonObject sendMsg = new JsonObject().put("test", "me");

        vertxMessageConsumer.handleEvent(msgHandler -> {
            JsonObject msg = msgHandler.toJsonObject();
            context.assertNotNull(msg);
            context.assertTrue(msg.getString("test").equals("me"));
            async.complete();
        });

        vertx.eventBus().send(address, sendMsg.toBuffer());
    }
}
