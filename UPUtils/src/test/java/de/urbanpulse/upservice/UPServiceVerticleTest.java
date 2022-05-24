
package de.urbanpulse.upservice;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UPServiceVerticleTest {

    private static final String SID = UUID.randomUUID().toString();
    private static final String EVENT_TYPE_NAME = "test";
    private static final JsonObject SAMPLE_RESULT_1 = new JsonObject().put("id", SID);
    private static final JsonObject SAMPLE_RESULT_2 = new JsonObject().put("name", EVENT_TYPE_NAME);

    @Mock
    private JDBCClient client;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private UPServiceVerticle eventTypeResolver;

    @Test
    public void test_getSIDsForEventTypeId_willExecuteCorrectQuery() {
        prepareMockedSIDResponse(SAMPLE_RESULT_1);
        eventTypeResolver.getSIDsForEventTypeId("eventTypeId", res -> {
            assertTrue(res.succeeded());
            List<String> list = res.result();
            assertEquals(1, list.size());
            assertEquals(SID, list.get(0));
            verify(client, times(1)).queryWithParams(eq(UPServiceVerticle.EVENT_TYPE_ID_TO_SIDS_QUERY), any(JsonArray.class), any(Handler.class));
        });
    }

    @Test
    public void test_getSIDsForEventTypeName_willExecuteCorrectQuery() {
        prepareMockedSIDResponse(SAMPLE_RESULT_1);
        eventTypeResolver.getSIDsForEventTypeName("eventTypeId", res -> {
            assertTrue(res.succeeded());
            List<String> list = res.result();
            assertEquals(1, list.size());
            assertEquals(SID, list.get(0));
            verify(client, times(1)).queryWithParams(eq(UPServiceVerticle.EVENT_TYPE_NAME_TO_SIDS_QUERY), any(JsonArray.class), any(Handler.class));
        });
    }

    @Test
    public void test_getEventTypeNameForEventTypeId(){
        prepareMockedSIDResponse(SAMPLE_RESULT_2);
        eventTypeResolver.getEventTypeNameForEventTypeId("test1", res -> {
            assertTrue(res.succeeded());
            String name = res.result();
            assertNotNull(name);
            assertEquals(EVENT_TYPE_NAME, name);
            verify(client, times(1)).queryWithParams(eq(UPServiceVerticle.EVENT_TYPE_NAME_FOR_EVENT_TYPE_ID_QUERY), any(JsonArray.class), any(Handler.class));
        });
    }


    private void prepareMockedSIDResponse(JsonObject responseObj){
        doAnswer(invocation -> {
            Handler<AsyncResult<ResultSet>> resultSetHandler = invocation.getArgument(2);
            resultSetHandler.handle(Future.succeededFuture(resultSet));
            return null;
        }).when(client).queryWithParams(anyString(), any(JsonArray.class), any(Handler.class));

        given(resultSet.getRows()).willReturn(Collections.singletonList(responseObj));
    }
}
