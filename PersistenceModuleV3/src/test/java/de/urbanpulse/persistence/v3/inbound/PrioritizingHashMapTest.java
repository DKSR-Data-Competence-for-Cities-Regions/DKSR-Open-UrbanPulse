package de.urbanpulse.persistence.v3.inbound;

import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class PrioritizingHashMapTest {

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    /**
     * Test of drainMostImportant method, of class PrioritizingHashMap.
     * @param context TestContext
     * @throws java.lang.InterruptedException if something got interrupted
     */
    @Test
    public void testEmptyQueuesAreNotReturned(TestContext context) throws InterruptedException {
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);
        map.put("bla", createQueue());
        Async async1 = context.async();
        vertx.setTimer(100, i -> async1.complete());
        async1.await();
        map.put("blub", createQueue());
        Async async2 = context.async();
        vertx.setTimer(100, i -> async2.complete());
        async2.await();
        map.put("foo", new LinkedBlockingQueue<>());
        Async async3 = context.async();
        vertx.setTimer(100, i -> async3.complete());
        async3.await();
        map.put("bar", createQueue());

        final ArrayList<JsonObject> drainedElements = new ArrayList<>();
        Optional<String> sid = map.drainMostImportant(drainedElements);
        assertEquals("bla", sid.get());
        assertEquals(2, drainedElements.size());
        assertEquals(23, drainedElements.get(0).getInteger("a").intValue());
        assertEquals(42, drainedElements.get(1).getInteger("b").intValue());
        sid = map.drainMostImportant(drainedElements);
        assertEquals("blub", sid.get());
        sid = map.drainMostImportant(drainedElements);
        assertEquals("bar", sid.get());
        sid = map.drainMostImportant(drainedElements);
        assertEquals(Optional.empty(), sid); // because "foo" has no elements
    }

    public LinkedBlockingQueue<JsonObject> createQueue() {
        LinkedBlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
        queue.add(new JsonObject("{\"a\": 23}"));
        queue.add(new JsonObject("{\"b\": 42}"));
        return queue;
    }

    /**
     * Test of drainMostImportant method, of class PrioritizingHashMap.
     * @param context TestContext
     * @throws java.lang.InterruptedException if something got interrupted
     */
    @Test
    public void testMaxElements(TestContext context) throws InterruptedException {
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
        queue.add(new JsonObject("{\"one\": 1}"));
        map.put("foo", queue);

        Async async = context.async();
        vertx.setTimer(50, i -> async.complete());
        async.await();

        queue = new LinkedBlockingQueue<>();
        queue.add(new JsonObject("{\"a\": 23}"));
        queue.add(new JsonObject("{\"b\": 42}"));
        queue.add(new JsonObject("{\"c\": 128}"));
        queue.add(new JsonObject("{\"d\": 901171053393}"));
        map.put("bar", queue);
        map.setMaxElements(2);

        final ArrayList<JsonObject> drainedElements = new ArrayList<>();
        Optional<String> sid = map.drainMostImportant(drainedElements);
        assertEquals("bar", sid.get()); // because it has more elements than allowed
        sid = map.drainMostImportant(drainedElements);
        assertEquals("foo", sid.get());
    }

    /**
     * Tests that no events remain in the map once drainMostImportant has been called often enough.
     */
    @Test
    public void testAllEventsAreDrainedEventually() {
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        for (int i = 0; i < 10; i++) {
            LinkedBlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
            for (int j = 0; j < 10000; j++) {
                queue.add(new JsonObject().put("foo", "bar" + j));
            }
            map.put("sid-" + i, queue);
        }

        // Now we have 10 queues à 10000 events in there, so 100000. So each drain should reduce the size by 100,
        // resulting in 0 at the end
        for (int i = 0; i < 1000; i++) {
            map.drainMostImportant(new ArrayList<>());
            assertEquals(100000 - (i + 1) * 100, map.getTotalCount());
        }
    }

    @Test
    public void testPrioritization_5_seconds_trumps_max_elements(TestContext context) throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        queue1.add(new JsonObject());
        map.put("sid-1", queue1);
        vertx.setTimer(6000, i -> {
            IntStream.range(0, 100).forEach(ig -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);

            context.assertEquals("sid-1", map.determineNextDrainee().get().getKey());
            async.complete();
        });
    }

    @Test
    public void testPrioritization_5_seconds_both_is_sorted_by_age(TestContext context)
            throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        queue1.add(new JsonObject());
        map.put("sid-1", queue1);
        vertx.setTimer(100, ig -> {
            IntStream.range(0, 100).forEach(i -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);
            vertx.setTimer(6000, ign -> {
                context.assertEquals("sid-1", map.determineNextDrainee().get().getKey());
                async.complete();
            });
        });
    }

    @Test
    public void testPrioritization_max_elements_exceeded_trumps_time(TestContext context) throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        queue1.add(new JsonObject());
        map.put("sid-1", queue1);
        vertx.setTimer(1000, i -> {
            IntStream.range(0, 101).forEach(ig -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);

            context.assertEquals("sid-2", map.determineNextDrainee().get().getKey());
            async.complete();
        });
    }

    @Test
    public void testPrioritization_both_max_elements_exceeded_is_sorted_by_size(TestContext context) throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        IntStream.range(0, 101).forEach(i -> queue1.add(new JsonObject()));
        map.put("sid-1", queue1);
        vertx.setTimer(1000, i -> {
            IntStream.range(0, 105).forEach(ig -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);

            context.assertEquals("sid-2", map.determineNextDrainee().get().getKey());
            async.complete();
        });
    }

    @Test
    public void testPrioritization_max_elements_and_equal_size_is_sorted_by_age(TestContext context) throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        IntStream.range(0, 105).forEach(i -> queue1.add(new JsonObject()));
        map.put("sid-1", queue1);
        vertx.setTimer(1000, i -> {
            IntStream.range(0, 105).forEach(ig -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);

            context.assertEquals("sid-1", map.determineNextDrainee().get().getKey());
            async.complete();
        });
    }

    @Test
    public void testPrioritizaton_all_else_is_sorted_by_age(TestContext context) throws InterruptedException {
        Async async = context.async();
        PrioritizingHashMap map = new PrioritizingHashMap(100, 5000);

        LinkedBlockingQueue<JsonObject> queue1 = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<JsonObject> queue2 = new LinkedBlockingQueue<>();

        IntStream.range(0, 99).forEach(i -> queue1.add(new JsonObject()));
        map.put("sid-1", queue1);
        vertx.setTimer(1000, i -> {
            IntStream.range(0, 5).forEach(ig -> queue2.add(new JsonObject()));
            map.put("sid-2", queue2);

            context.assertEquals("sid-1", map.determineNextDrainee().get().getKey());
            async.complete();
        });
    }

    @Test
    public void test_equals_multiple_use_cases(){
        PrioritizingHashMap map1 = new PrioritizingHashMap(100, 5000);
        PrioritizingHashMap sameMaps = new PrioritizingHashMap(100, 5000);
        PrioritizingHashMap differentKeyElementNameMap = new PrioritizingHashMap(100, 5000);
        PrioritizingHashMap noElementsMap = new PrioritizingHashMap(100, 5000);
        PrioritizingHashMap differentAmountOfMaxElements = new PrioritizingHashMap(10, 5000);

        map1.put("foo", new LinkedBlockingQueue<>());
        differentKeyElementNameMap.put("bar", new LinkedBlockingQueue<>());
        differentAmountOfMaxElements.putAll(map1);
        sameMaps.putAll(map1);

        //failing
        assertNotEquals(map1, differentKeyElementNameMap);
        assertNotEquals(map1, noElementsMap);
        assertNotEquals(map1, differentAmountOfMaxElements);
        assertNotEquals(map1, null);
        assertNotEquals(map1, new ArrayList<>());

        //success
        assertEquals(map1, map1);
        assertEquals(map1, sameMaps);
    }
}
