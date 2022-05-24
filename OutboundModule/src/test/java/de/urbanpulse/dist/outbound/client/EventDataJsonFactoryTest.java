package de.urbanpulse.dist.outbound.client;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EventDataJsonFactoryTest {

    private EventDataJsonFactory factory;

    private JsonObject event1in;
    private JsonObject event2in;

    private JsonObject event1out;
    private JsonObject event2out;

    private List<JsonObject> input;

    private JsonObject expected;

    @Before
    public void setUp() {
        factory = new EventDataJsonFactory();
        input = new LinkedList<>();

        event1out = new JsonObject();
        event1out.put("SID", "V1");
        event1out.put("timestamp", "2016-02-18T05:42:04.629+0000");
        event1out.put("value", 13.7);

        event1in = event1out.copy().put("statementName", "myStatement");

        event2out = new JsonObject();
        event2out.put("SID", "V1");
        event2out.put("timestamp", "2016-02-19T05:54:31.198+0000");
        event2out.put("value", -3.8);

        event2in = event2out.copy().put("statementName", "myStatement");

        input.add(event1in);
        input.add(event2in);

        expected = new JsonObject();

        JsonArray messages = new JsonArray();


        JsonObject intermediate = new JsonObject();
        JsonArray events = new JsonArray();
        events.add(event1out);
        events.add(event2out);

        intermediate.put("statement", "myStatement");
        intermediate.put("event", events);

        messages.add(intermediate);

        expected.put("messages", messages);
    }

    @Test
    public void testBuildEventDataJson() {
        StringBuilder builder = factory.buildEventDataJson(input);
        JsonObject result = new JsonObject(builder.toString());

        assertEquals(expected, result);
    }

}
