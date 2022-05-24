package de.urbanpulse.dist.outbound.server.historicaldata;

import city.ui.shared.commons.time.UPDateTimeFormat;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class QueryFilteringTest {
    private final String sid = "7";
    private JsonObject config;
    private QueryFiltering queryFiltering;

    @Before
    public void setUp() {
        JsonObject requestFilters = new JsonObject()
                .put("maxIntervalSizeInMinutes", 100080)
                .put("maxAgeInDays", 1);
        JsonObject eventFilters = new JsonObject().put("eventParameterExcludeFilter", new JsonArray().add("encrpyt"));

        config = new JsonObject().put("sidFilters", new JsonObject()
                .put(sid, new JsonObject().put("requestFilters", requestFilters).put("eventFilters", eventFilters)));
        List<String> sidsList = new ArrayList<>();
        sidsList.add(sid);

        queryFiltering = new QueryFiltering(config, sidsList);
    }


    @Test
    public void testSimpleFilteringNoSinceUntil() {
        List<String> filteredListNoSinceAndUntil = queryFiltering.filter(null, null);
        assertEquals(1, filteredListNoSinceAndUntil.size());
        assertEquals(sid, filteredListNoSinceAndUntil.get(0));

        List<String> filteredListSinceMissing = queryFiltering.filter(null, "time");
        assertEquals(0, filteredListSinceMissing.size());
    }

    @Test
    public void testSinceUntilFiltering(){
        String since = ZonedDateTime.now().minusHours(1).format(UPDateTimeFormat.getFormatter());
        String until = ZonedDateTime.now().format(UPDateTimeFormat.getFormatter());
        List<String> filteredListWithSinceAndUntil = queryFiltering.filter(since, until);
        assertEquals(1, filteredListWithSinceAndUntil.size());
        assertEquals(sid, filteredListWithSinceAndUntil.get(0));


        String sinceAfterUntil = ZonedDateTime.now().plusHours(1).format(UPDateTimeFormat.getFormatter());
        List<String> filteredListSinceAfterUntil = queryFiltering.filter(sinceAfterUntil, until);
        assertEquals(0, filteredListSinceAfterUntil.size());

        String sinceBiggerThan_maxAgeInDays = ZonedDateTime.now().minusDays(2).format(UPDateTimeFormat.getFormatter());
        List<String> filteredListSinceAfterMaxAgeInDays = queryFiltering.filter(sinceBiggerThan_maxAgeInDays, until);
        assertEquals(0, filteredListSinceAfterMaxAgeInDays.size());
    }

    @Test
    public void testApplyFiltersWithExcludeParameter(){
        JsonObject exampleExcludeEvent = new JsonObject().put("SID", sid).put("test", 4).put("encrpyt", false);
        JsonObject filteredEvent = queryFiltering.applyEventFilter(exampleExcludeEvent, null);

        assertEquals(new JsonObject().put("SID", sid).put("test", 4), filteredEvent);
    }

    @Test
    public void testApplyFiltersWithoutExcludeParameter(){
        config.getJsonObject("sidFilters").getJsonObject(sid).remove("eventFilters");

        List<String> sidsList = new ArrayList<>();
        sidsList.add(sid);

        QueryFiltering queryFiltering2 = new QueryFiltering(config, sidsList);

        JsonObject exampleExcludeEvent = new JsonObject().put("SID", sid).put("test", 4).put("encrpyt", false);

        JsonObject filteredEvent = queryFiltering2.applyEventFilter(exampleExcludeEvent, null);
        assertEquals(new JsonObject().put("SID", sid).put("test", 4).put("encrpyt", false), filteredEvent);


        Set<String> includesOnly = new HashSet<>();
        includesOnly.add("test");
        JsonObject filteredEventIncludesOnly = queryFiltering2.applyEventFilter(exampleExcludeEvent, includesOnly);
        assertEquals(new JsonObject().put("test", 4), filteredEventIncludesOnly);
    }

}
