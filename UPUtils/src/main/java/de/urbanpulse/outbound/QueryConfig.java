package de.urbanpulse.outbound;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@DataObject
public class QueryConfig {

    private List<String> sids;
    private String since;
    private String until;

    public QueryConfig() {
    }

    public QueryConfig(JsonObject config) {
        sids = config.getJsonArray("sids").stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
        since = config.getString("since");
        until = config.getString("until");
    }

    public boolean isRangeQuery() {
        return since != null && until != null;
    }

    public String getSince() {
        return since;
    }

    public QueryConfig setSince(String since) {
        this.since = since;
        return this;
    }

    public String getUntil() {
        return until;
    }

    public QueryConfig setUntil(String until) {
        this.until = until;
        return this;
    }

    public List<String> getSids() {
        return sids;
    }

    public QueryConfig setSids(List<String> sids) {
        this.sids = sids;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject(Json.encode(this));
    }
}
