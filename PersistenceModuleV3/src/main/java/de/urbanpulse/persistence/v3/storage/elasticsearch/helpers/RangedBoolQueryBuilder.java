package de.urbanpulse.persistence.v3.storage.elasticsearch.helpers;

import java.time.ZonedDateTime;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * @author <a href="iliya.bahchevanski@the-urban-institute.de">Iliya Bahchevanski</a>
 */
public class RangedBoolQueryBuilder {

    private RangedBoolQueryBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static SearchSourceBuilder createRangedBooleanQuery(ZonedDateTime since, ZonedDateTime until, List<String> sids) {
        RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder("timestamp");
        rangeQueryBuilder.from(since, true);
        rangeQueryBuilder.to(until, true);
        
         TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("SID.keyword", sids);
        
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(rangeQueryBuilder);
        boolQueryBuilder.must(termsQueryBuilder);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        return searchSourceBuilder;
    }
}
