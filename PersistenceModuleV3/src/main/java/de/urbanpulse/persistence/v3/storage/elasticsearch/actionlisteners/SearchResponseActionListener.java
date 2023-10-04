package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SearchResponseActionListener implements ActionListener<SearchResponse> {

    private Context context;
    private Promise<String> responsePromise;

    public SearchResponseActionListener(Context context, Promise<String> responsePromise) {
        this.context = context;
        this.responsePromise = responsePromise;
    }

    @Override
    public void onResponse(SearchResponse searchResponse) {
        context.runOnContext(hndlr -> {
            if (searchResponse.status().getStatus() == 200) {
                SearchHit[] searchHits = searchResponse.getHits().getHits();

                if (searchHits.length > 0) {
                    String indexName = (String) searchHits[0].getSourceAsMap().get(ElasticSearchSecondLevelStorageServiceImpl.UP_INDEX_FIELD_NAME);
                    responsePromise.complete(indexName);
                } else {
                    responsePromise.fail("No Index found!");
                }
            } else {
                responsePromise.fail("Search query for Index returned unwanted status code "
                        + searchResponse.status().getStatus());
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> responsePromise.fail(e));
    }
}
