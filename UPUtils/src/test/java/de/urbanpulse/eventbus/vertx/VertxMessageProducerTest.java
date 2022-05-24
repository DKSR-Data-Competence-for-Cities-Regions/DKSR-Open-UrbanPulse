package de.urbanpulse.eventbus.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
public class VertxMessageProducerTest {
    private Vertx vertx;
    private VertxMessageProducer vertxMessageProducer;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        vertxMessageProducer = new VertxMessageProducer(vertx);
    }

    @Test
    public void send(TestContext context) {
        Async async = context.async();
        JsonObject message = new JsonObject().put("test", "me");
        String target = "test.target";

        vertx.eventBus().localConsumer(target, hndlr -> {
            Buffer buffer = (Buffer) hndlr.body();
            JsonObject receivedMsg = buffer.toJsonObject();
            context.assertNotNull(receivedMsg);
            context.assertTrue(receivedMsg.getString("test").equals("me"));
            async.complete();
        });

        vertxMessageProducer.send(target, message.toBuffer(), null);
    }
}
