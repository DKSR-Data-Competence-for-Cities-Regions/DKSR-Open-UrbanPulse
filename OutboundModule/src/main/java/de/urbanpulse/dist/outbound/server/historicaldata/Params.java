
package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Set;

/**
 * Just a DTO for a lot of parameters that are being passed within the {@link HistoricalDataRestVerticle}
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class Params {
    Set<String> includeOnly;
    JsonObject roleConfiguration;
    String since;
    String until;
    HttpServerResponse response;
    OutputFormat outputFormat;
    RoutingContext routingContext;

    Params setIncludeOnly(Set<String> includeOnly) {
        this.includeOnly = includeOnly;
        return this;
    }

    Params setRoleConfiguration(JsonObject roleConfiguration) {
        this.roleConfiguration = roleConfiguration;
        return this;
    }

    Params setSince(String since) {
        this.since = since;
        return this;
    }

    Params setUntil(String until) {
        this.until = until;
        return this;
    }

    Params setResponse(HttpServerResponse response) {
        this.response = response;
        return this;
    }

    Params setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    Params setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
        return this;
    }
}
