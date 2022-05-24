package de.urbanpulse.persistence.v3.outbound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class BatchSenderTest {

    private Vertx vertx = null;

    public BatchSenderTest() {
    }

    @Before
    public void setUp(TestContext context) throws IOException {
        this.vertx = Vertx.vertx();
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async();
        vertx.close(result -> {
            async.complete();
        });
    }

    @Test
    public void testBackPressure_bug8189(TestContext context) {

        BatchSender batchSender = new BatchSender(vertx);
        String uniqueAddress = "UNIQUE_ADDRESS";

        Async async = context.async();
        AtomicInteger batchCounter = new AtomicInteger(0);

        vertx.eventBus().consumer(uniqueAddress, message -> {
            JsonObject jsonMessage = (JsonObject) message.body();
            System.out.println(jsonMessage);
            batchCounter.getAndIncrement();
            if (!jsonMessage.containsKey("isLast")) {
                message.reply("sendNextBatch");
            } else {
                message.reply("done");
                context.assertEquals(3, batchCounter.get());
                async.complete();
            }

        });
        List<JsonObject> list = new ArrayList<>();
        IntStream.range(0, 3).forEach(i -> list.add(new JsonObject()));
        batchSender.sendIteratorResultsInBatches(list.iterator(), 1, uniqueAddress, false, h -> {
            context.assertTrue(h.succeeded());
        });

    }

}
