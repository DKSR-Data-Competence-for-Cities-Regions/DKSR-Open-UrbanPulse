package de.urbanpulse.cep;

import com.espertech.esper.client.EventBean;
import de.urbanpulse.eventbus.vertx.VertxMessageProducer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class EsperUpdateListenerVertxTest {

    @Mock
    EventBeanConverter eventBeanConverter;

    @InjectMocks
    EsperUpdateListenerVertx updateListenerVertx;

    private Vertx vertx;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        vertx = Vertx.vertx();
    }

    @After
    public void stop() {
        vertx.close();
    }

    /**
     * Test of update method, of class EsperUpdateListenerVertx.
     * @param context The test context
     */
    @Test
    public void testUpdate(TestContext context) {
        Async a = context.async();
        EventBean[] ebs = null;
        EventBean[] ebs1 = null;

        JsonArray array = new JsonArray()
                .add(new JsonObject()
                        .put("SID", "a")
                        .put("timestamp", "123"))
                .add(new JsonObject()
                        .put("SID", "b")
                        .put("timestamp", "456"));
        //If the update method gets triggered by Esper we should receive the events here.
        vertx.eventBus().consumer("thePersistence", (Message<Buffer> m) -> {
            context.assertEquals(array, m.body().toJsonArray());
            a.complete();
        });

        //here the ebs from Esper gets transformed into a json array.
        when(eventBeanConverter.toJsonArray(any(), any())).thenReturn(array);
        updateListenerVertx = new EsperUpdateListenerVertx(vertx, eventBeanConverter, new VertxMessageProducer(vertx),
                "foo",new JsonArray().add("thePersistence"));
        //Here we just trigger the process.
        updateListenerVertx.update(ebs, ebs1);

    }

}
