package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CSVRowWriterTest {

    @InjectMocks
    private CSVRowWriter writer;

    @Mock
    private HttpServerResponse response;

    private StringBuilder resultBuilder;

    private static final String EXPECTED_CSV = "SID;timestamp;\"also#escaped\"\"\";dvalue;escaped" + System.lineSeparator()
            + "13;2016-11-29T14:43:51.231Z;bla;1.2;\"hello;world#fooÖÄÜ\"" + System.lineSeparator()
            + "7;2016-11-29T14:50:14.863Z;;-5.1;" + System.lineSeparator();

    @Before
    public void setUp() {
        resultBuilder = new StringBuilder();
        given(response.write(Matchers.anyString(), eq("UTF-8"))).willAnswer((InvocationOnMock invocation) -> {
            resultBuilder.append((String) invocation.getArguments()[0]);
            return response;
        });
    }

    @Test
    public void testWriteCsvEvents() {
        AtomicBoolean isFirst = new AtomicBoolean(true);

        JsonObject event1 = new JsonObject();
        event1.put("also#escaped\"", "bla");
        event1.put("timestamp", "2016-11-29T14:43:51.231Z");
        event1.put("dvalue", 1.2);
        event1.put("SID", "13");
        event1.put("escaped", "hello;world#fooÖÄÜ");

        JsonObject event2 = new JsonObject();
        event2.put("ignoredAsNotPresentInFirstEvent", 4711);
        event2.put("SID", "7");
        event2.put("dvalue", -5.1);
        event2.put("timestamp", "2016-11-29T14:50:14.863Z");

        CSVHeaderSet headerFields = new CSVHeaderSet();
        writer.writeCsvEvent(isFirst, response, event1, headerFields);
        writer.writeCsvEvent(isFirst, response, event2, headerFields);

        String actualResult = resultBuilder.toString();
        System.out.println(actualResult);
        assertEquals(EXPECTED_CSV, actualResult);
    }

}
