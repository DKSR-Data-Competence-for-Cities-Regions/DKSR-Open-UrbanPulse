package de.urbanpulse.persistence.v3.storage.elasticsearch.helpers;

import java.util.List;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class LatestEventTermQueryBuilder {

    public static SearchSourceBuilder createTermQueryBuilder(List<String> sids, String innerHitKeyName) {
        TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("SID.keyword", sids);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(termsQueryBuilder);

        FieldSortBuilder fieldSortBuilder = SortBuilders.fieldSort("timestamp").order(SortOrder.DESC);

        InnerHitBuilder innerHitBuilder = new InnerHitBuilder(innerHitKeyName).setSize(1).addSort(fieldSortBuilder);
        CollapseBuilder collapseBuilder = new CollapseBuilder("SID.keyword").setInnerHits(innerHitBuilder);

        searchSourceBuilder.collapse(collapseBuilder);
        searchSourceBuilder.size(sids.size());

        return searchSourceBuilder;
    }

    private LatestEventTermQueryBuilder() {
        // Not intended to be instantiated.
    }
}
