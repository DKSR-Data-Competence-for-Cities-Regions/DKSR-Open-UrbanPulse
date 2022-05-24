package de.urbanpulse.cep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class EventBeanConverter {

    private final DateTimeFormatter ISO_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final String VS_RESULT_STATEMENT_SUFFIX = "_VSResultStatement";

    public JsonArray toJsonArray(EventBean[] ebArray, String statement) {
        JsonArray array = new JsonArray();
        for (EventBean eventBean : ebArray) {
            array.add(toJsonObject(eventBean, statement));
        }
        return array;
    }

    private JsonObject toJsonObject(EventBean b, String statement) {
        Map<String, Object> map = new HashMap<>();
        EventType t = b.getEventType();
        for (String prop : t.getPropertyNames()) {
            final Object propertyValue = b.get(prop);
            if (propertyValue instanceof java.util.Date) {
                Date date = (Date) propertyValue;
                map.put(prop, ISO_FORMATTER.print(new DateTime(date).withZone(DateTimeZone.UTC)));
            } else {
                map.put(prop, propertyValue);
            }
        }
        JsonObject jsonObject = new JsonObject(map);
        if (statement.endsWith(VS_RESULT_STATEMENT_SUFFIX)){
            JsonObject headers = jsonObject.getJsonObject("_headers", new JsonObject()).put("eventType", statement.split(VS_RESULT_STATEMENT_SUFFIX)[0]);
            jsonObject.put("_headers", headers);
        }
        return jsonObject.put("statementName", statement);
    }
}
