package de.urbanpulse.outbound;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class QueryConfigTest {

    @Test
    public void testQeuryConfigConstructor() {
        JsonObject config = new JsonObject().put("sids", new JsonArray().add("7").add("3"))
                .put("since", "12.09.2020")
                .put("until", "13.09.2020");

        QueryConfig queryConfig = new QueryConfig(config);

        assertTrue(queryConfig.isRangeQuery());
        assertEquals(2, queryConfig.getSids().size());
    }

    @Test
    public void testQeuryConfigSetters() {
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setSids(Arrays.asList("2", "3"));

        assertFalse(queryConfig.isRangeQuery());
        assertEquals(2, queryConfig.getSids().size());
    }

}
