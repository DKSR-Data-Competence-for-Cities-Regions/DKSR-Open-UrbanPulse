package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SearchAfterSearchResponseActionListener implements ActionListener<SearchResponse> {

    private final Context context;
    private final Promise<JsonObject> responsePromise;

    public SearchAfterSearchResponseActionListener(Context context, Promise<JsonObject> responsePromise) {
        this.context = context;
        this.responsePromise = responsePromise;
    }

    @Override
    public void onResponse(SearchResponse searchResponse) {
        context.runOnContext(hndlr -> {
            if (searchResponse.status().getStatus() == 200) {
                SearchHit[] searchHits = searchResponse.getHits().getHits();
                JsonArray batchData = new JsonArray();
                String lastUpHash = "empty";

                for (SearchHit hit : searchHits) {
                    JsonObject hitObj = new JsonObject(hit.getSourceAsString());

                    //the first request wont have a hash so to be able to get the hash we will need to extract it
                    //from the document that we have received  and not from the sort values
                    lastUpHash = (String) hitObj.remove(ElasticSearchSecondLevelStorageServiceImpl.UP_HASH_FIELD_NAME);

                    //clean some additional fields added by us
                    hitObj.remove(ElasticSearchSecondLevelStorageServiceImpl.UP_GEO_LOCATION_FIELD);
                    batchData.add(hitObj);
                }

                long lastDocumentTimeStamp;
                if (searchHits.length > 0) {
                    //this is the last timestamp from the hits response and will become the new
                    //timestamp for the next request
                    lastDocumentTimeStamp = (long) searchHits[searchHits.length - 1].getSortValues()[0];
                } else {
                    lastDocumentTimeStamp = 0;
                }

                responsePromise.complete(new JsonObject().put("batchData", batchData)
                        .put("lastTimeStamp", lastDocumentTimeStamp)
                        .put(ElasticSearchSecondLevelStorageServiceImpl.UP_HASH_FIELD_NAME, lastUpHash));
            } else {
                responsePromise.fail("Something went wrong requesting batch data from ElasticSearch status "
                        + searchResponse.status().getStatus());
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> responsePromise.fail(e));
    }
}
