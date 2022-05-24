package de.urbanpulse.persistence.v3.storage;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import city.ui.shared.commons.time.UPDateTimeFormat;
import io.vertx.core.json.JsonObject;

/**
 * Just some events that can be used for testing
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class PredefinedEvents {

    public static final ZonedDateTime time = ZonedDateTime.now();
    static final DateTimeFormatter TIMESTAMP_FORMATTER = UPDateTimeFormat.getFormatterWithZoneZ();
    public static JsonObject[] invalidObjects;
    public static JsonObject[] validObjects;

    static {
        JsonObject nullEvent = null;
        JsonObject emptyEvent = new JsonObject();
        JsonObject notAnEvent = new JsonObject("{\"bla\": \"blub\"}");
        JsonObject validEvent1 = new JsonObject("{\"SID\": \"23\", \"foo\": \"bar\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time)); // first valid value
        JsonObject validEvent2 = new JsonObject("{\"SID\": \"23\", \"foo\": \"bar\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time)); // duplicate of first value
        JsonObject validEvent3 = new JsonObject("{\"SID\": \"23\", \"foo\": \"bar\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time.plusSeconds(1))); // duplicate of first value with different timestamp
        JsonObject validEvent4 = new JsonObject("{\"SID\": \"23\", \"hihi\": \"hoho\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time)); // duplicate of first value with different data (except SID and timestamp)
        JsonObject validEvent5 = new JsonObject("{\"SID\": \"42\", \"tatü\": \"tata\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time)); // another valid value
        JsonObject invalidEvent1 = new JsonObject("{\"SID\": 42, \"tatü\": \"tata\"}").put("timestamp", TIMESTAMP_FORMATTER.format(time)); // SID is an int
        JsonObject invalidEvent2 = new JsonObject("{\"SID\": \"42\", \"tatü\": \"tata\"}"); // timestamp not given

        invalidObjects = new JsonObject[]{invalidEvent1, invalidEvent2, nullEvent, emptyEvent, notAnEvent};
        validObjects = new JsonObject[]{validEvent1, validEvent2, validEvent3, validEvent4, validEvent5};
    }

}
