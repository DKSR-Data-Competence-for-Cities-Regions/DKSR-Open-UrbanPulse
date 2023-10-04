package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class InnerHitsActionListener implements ActionListener<SearchResponse> {
    
    private final Context vertxContext;
    private final Promise<List<JsonObject>> responsePromise;
    private final String innerHitKey;
    
    public InnerHitsActionListener(Context context, String innerHitKey, Promise<List<JsonObject>> responsePromise) {
        this.vertxContext = context;
        this.responsePromise = responsePromise;
        this.innerHitKey = innerHitKey;
    }
    
    @Override
    public void onResponse(SearchResponse searchResponse) {
        vertxContext.runOnContext(handler -> processRespones(searchResponse));
        
    }
    
    @Override
    public void onFailure(Exception e) {
        vertxContext.runOnContext(handler -> responsePromise.fail(e));
    }
    
    protected void processRespones(SearchResponse searchResponse) {
        List<JsonObject> result = new ArrayList<>();
        if (searchResponse.status().getStatus() == 200) {
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            Arrays.<SearchHit>asList(searchHits).stream()
                    .map(hit -> hit.getInnerHits().get(innerHitKey))
                    .map(SearchHits::getHits)
                    .map(Arrays::asList).forEach(list -> {
                List<JsonObject> hitsAsJson = list.stream().map(SearchHit::getSourceAsString)
                        .map(JsonObject::new).map(this::cleanUp).collect(Collectors.toList());
                result.addAll(hitsAsJson);
            });
            this.responsePromise.complete(result);
        } else {
            responsePromise.fail("Something went wrong requesting inner hits from ElasticSearch "
                    + searchResponse.status().getStatus());
        }
    }
    
    private JsonObject cleanUp(JsonObject json) {
        json.remove(ElasticSearchSecondLevelStorageServiceImpl.UP_HASH_FIELD_NAME);
        json.remove(ElasticSearchSecondLevelStorageServiceImpl.UP_GEO_LOCATION_FIELD);
        return json;
    }
    
}
