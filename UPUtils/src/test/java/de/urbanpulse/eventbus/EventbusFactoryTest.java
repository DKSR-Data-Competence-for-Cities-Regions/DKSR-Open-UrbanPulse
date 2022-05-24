package de.urbanpulse.eventbus;

import de.urbanpulse.eventbus.vertx.VertxEventbusFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
public class EventbusFactoryTest {
    private Vertx vertx;
    private EventbusFactory factory;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @Test
    public void test_vertxEventBusClassCreation() {
        JsonObject config = new JsonObject().put("class", "de.urbanpulse.eventbus.vertx.VertxEventbusFactory");
        factory = EventbusFactory.createFactory(vertx, config);
        assertNotNull(factory);
        assertEquals(factory.getClass(), VertxEventbusFactory.class);
    }

    @Test
    public void test_vertxEventBusDefaultClassCreation() {
        factory = EventbusFactory.createFactory(vertx, new JsonObject());
        assertNotNull(factory);
        assertEquals(factory.getClass(), VertxEventbusFactory.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_vertxEventBusClassCreation_fails() {
        factory = EventbusFactory.createFactory(vertx, new JsonObject().put("class", "i.am.not.know.Class"));
        assertNotNull(factory);
    }

}
