package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.dist.outbound.MainVerticle;
import de.urbanpulse.dist.util.StatementConsumerManagementVerticle;
import de.urbanpulse.dist.util.UpdateListenerConfig;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsPublisherVerticleTest {

    private WsPublisherVerticle publisherVerticle;
    private Vertx vertxMock;
    private Context context;

    @Before
    public void setUp() {
        publisherVerticle = new WsPublisherVerticle();
        vertxMock = Mockito.mock(Vertx.class);
        context = Mockito.mock(Context.class);
        publisherVerticle.init(vertxMock, context);
    }

    /**
     * Test of start method, of class WsPublisherVerticle.
     * @throws java.lang.Exception
     */
    @Test
    public void testStart() throws Exception {
        EventBus eventBusMock = Mockito.mock(EventBus.class);
        when(vertxMock.eventBus()).thenReturn(eventBusMock);

        Promise<Void> promise = Promise.promise();
        publisherVerticle.start(promise);
        assertTrue(promise.future().succeeded());
        verify(eventBusMock, times(1)).localConsumer(eq(WsPublisherVerticle.SETUP_ADDRESS), any());
    }

    /**
     * Test of handleEvent method, of class WsPublisherVerticle.
     */
    @Test
    public void test_handleEvent() {
        EventBus eventBusMock = Mockito.mock(EventBus.class);
        when(vertxMock.eventBus()).thenReturn(eventBusMock);
        when(context.config()).thenReturn(new JsonObject());

        String statementName = "statementName";
        String address = MainVerticle.GLOBAL_STATEMENT_PREFIX + statementName;
        JsonObject event = new JsonObject().put("uni","corn");
        publisherVerticle.handleEvent(statementName, event);
        verify(eventBusMock, times(1)).publish(address, event);
    }


    /**
     * Test of reset method, of class WsPublisherVerticle.
     */
    @Test
    public void test_reset() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field statementToListenerMap = StatementConsumerManagementVerticle.class.getDeclaredField("statementToListenerMap");
        statementToListenerMap.setAccessible(true);
        ((Map<String, Set<UpdateListenerConfig>>) statementToListenerMap.get(publisherVerticle))
                .put("key", new HashSet<>());

        assertFalse(publisherVerticle.getStatementToListenerMap().isEmpty());
        publisherVerticle.reset();
        assertTrue(publisherVerticle.getStatementToListenerMap().isEmpty());
    }

}
