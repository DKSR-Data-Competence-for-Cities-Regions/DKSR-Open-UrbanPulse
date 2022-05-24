package de.urbanpulse.persistence.v3.storage;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.CRC32;

import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.persistence.v3.jpa.JPAEventEntity;
import io.vertx.core.json.JsonObject;


/**
 * generates various keys from a sensor event {@link JsonObject} to be used in a {@link de.urbanpulse.persistence.v3.azure.AzureEventEntity} / {@link JPAEventEntity}
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class KeyFactory {

    public static final String PARTITION_KEY_PATTERN = "yyyyMMddHH";
    public static final String ROW_KEY_PATTERN = "yyyyMMddHHmmssSSS";

    /**
     * @param event JSON object of a single sensor event, including a "timestamp" property in an ISO date format
     * @return timestamp formatted via {@link #PARTITION_KEY_PATTERN}, to be used as partition key
     */
    public String createPartititonKey(JsonObject event) {
        ZonedDateTime date = ZonedDateTime.parse(event.getString("timestamp"), UPDateTimeFormat.getFormatterWithZoneZ());
        return formatDate(date, PARTITION_KEY_PATTERN);
    }

    public String formatDate(final ZonedDateTime date, final String pattern) {
        DateTimeFormatter partitionKeyPattern = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of("UTC"));
        return date.format(partitionKeyPattern);
    }

    /**
     * Reverse function to formatDate. Takes a string containing a date, and the pattern it was converted with
     * @param dateWithOptionalSuffix A date string with optional suffix like "_TheSID" used in row keys
     * @param pattern A string pattern
     * @return The converted DateTime object
     */
    public static ZonedDateTime unFormatDate(String dateWithOptionalSuffix, String pattern) {
        String date = dateWithOptionalSuffix.substring(0, pattern.length());
        DateTimeFormatter formatPattern = DateTimeFormatter.ofPattern(pattern);
        return ZonedDateTime.parse(date, formatPattern);
    }

    /**
     * @param event JSON object of a single sensor event, including the required parameter "SID" (Sensor ID) and "timestamp"
     * @return timestamp formatted via {@link #ROW_KEY_PATTERN}, to be used as row key
     */
    public String createRowKey(JsonObject event) {
        ZonedDateTime date = ZonedDateTime.parse(event.getString("timestamp"),UPDateTimeFormat.getFormatterWithZoneZ());
        String eventHash = createEventHash(event);
        return createRowKey(date, eventHash);
    }

    public String createRowKey(ZonedDateTime date, String postfix) {
        String dateRowKey = formatDate(date, ROW_KEY_PATTERN);
        return String.format("%s_%s", dateRowKey, postfix);
    }

    public String createRowKey(ZonedDateTime date) {
        return formatDate(date, ROW_KEY_PATTERN);
    }

    @Deprecated
    public String createRowKeyDeprecated(JsonObject event) {
        ZonedDateTime date = ZonedDateTime.parse(event.getString("timestamp"));
        String sid = createSid(event);
        String dateRowKey = formatDate(date, ROW_KEY_PATTERN);
        return String.format("%s_%s", dateRowKey, sid);
    }

    /**
     * @param event JSON object of a single sensor event, including the required parameter "SID" (Sensor ID) and "timestamp"
     * @return SID (sensor ID).
     */
    public String createSid(JsonObject event) {
        return event.getString("SID");
    }

    public String createEventHash(JsonObject event) {
        CRC32 crc = new CRC32();
        crc.update(event.encode().getBytes(Charset.forName("UTF-8")));
        long v = crc.getValue();
        return Long.toHexString(v);
    }
}
