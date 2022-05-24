package de.urbanpulse.dist.outbound.client;

import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EventDataJsonFactory {

    /**
     * @param events list with identical statementNames, e.g.
     * <pre>
     *  {
     *     "statementName": "myStatement",
     *     "value" : 13.7,
     *     "SID" : "V1",
     *     "timestamp" : "2016-02-18T05:42:04.629+0000"
     *   },
     *   {
     *     "statementName": "myStatement",
     *     "value" : -3.8,
     *     "SID" : "V1",
     *     "timestamp" : "2016-02-19T05:54:31.198+0000"
     *   }
     * </pre>
     *
     *
     * @return StringBuilder containing JSON of an object like this:
     * <pre>
     *    {
     *      "messages" : [ {
     *        "statement" : "myStatement",
     *       "events" : [ {
     *          "value" : 13.7,
     *          "SID" : "V1",
     *          "timestamp" : "2016-02-18T05:42:04.629+0000"
     *        }, {
     *        "value" : -3.8,
     *          "SID" : "V1",
     *          "timestamp" : "2016-02-19T05:54:31.198+0000"
     *        } ]
     *      } ]
     *    }
     * </pre>
     */
    public StringBuilder buildEventDataJson(List<JsonObject> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"messages\":[{\"statement\":\"");

        boolean first = true;
        for (JsonObject jsonObj : events) {
            if (first) {
                first = false;
                builder.append(jsonObj.getString("statementName"));
                builder.append("\",\"event\":[");
            } else {
                builder.append(",");
            }
            JsonObject copy = jsonObj.copy();
            copy.remove("statementName");
            builder.append(copy.encode());
        }

        builder.append("]}]}");
        return builder;
    }

    public StringBuilder buildEventDataJson_old(List<JsonObject> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"messages\":[");
        boolean first = true;
        for (JsonObject jsonObj : events) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(jsonObj.encode());
        }
        builder.append("]}");
        return builder;
    }
}
