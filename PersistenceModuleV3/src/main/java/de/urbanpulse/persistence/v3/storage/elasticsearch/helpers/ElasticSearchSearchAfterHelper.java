package de.urbanpulse.persistence.v3.storage.elasticsearch.helpers;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import java.time.ZonedDateTime;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import de.urbanpulse.persistence.v3.storage.elasticsearch.clients.ElasticSearchRestHighLevelClientWrapper;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchSearchAfterHelper {

    protected ElasticSearchRestHighLevelClientWrapper client;
    protected SearchRequest searchRequest;
    private List<String> sids;
    private ZonedDateTime since;
    private ZonedDateTime until;
    private final Context context;

    public ElasticSearchSearchAfterHelper(ElasticSearchRestHighLevelClientWrapper client, SearchRequest searchRequest,
            List<String> sids, ZonedDateTime since, ZonedDateTime until, Context context) {
        this.client = client;
        this.sids = sids;
        this.since = since;
        this.until = until;
        this.searchRequest = searchRequest;
        this.context = context;
    }

    public Future<JsonObject> searchAfterQuery(long fromTimeStamp, String upHash, int batchSize) {
        SearchSourceBuilder searchSourceBuilder = RangedBoolQueryBuilder.createRangedBooleanQuery(since, until, sids);
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);
        //when we make the first request we dont have a hash
        //so we get the first document and will use its hash for the next request
        if (upHash == null) {
            searchSourceBuilder.size(1);
            searchSourceBuilder.searchAfter(new Object[]{fromTimeStamp});
        } else {
            searchSourceBuilder.sort(ElasticSearchSecondLevelStorageServiceImpl.UP_HASH_FIELD_NAME + ".keyword", SortOrder.ASC);
            searchSourceBuilder.searchAfter(new Object[]{fromTimeStamp, upHash});
            searchSourceBuilder.size(batchSize);
        }

        searchRequest.source(searchSourceBuilder);
        return client.searchAfterAsync(searchRequest, context);
    }
}
