package de.urbanpulse.persistence.v3.jpa;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import de.urbanpulse.persistence.v3.storage.KeyFactory;
import io.vertx.core.json.JsonObject;

/**
 * creates {@link JPAEventEntity}s
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class JPAEventEntityFactory {

    private final KeyFactory keyFactory;

    public JPAEventEntityFactory() {
        this.keyFactory = new KeyFactory();
    }

    /**
     * @param event {@link JsonObject} representing a sensor event
     * properties
     * @return {@link JPAEventEntity}
     */
    public JPAEventEntity create(JsonObject event) {
        Timestamp partitionKey = convertKeyToSQLTimestamp(keyFactory.createPartititonKey(event), KeyFactory.PARTITION_KEY_PATTERN);
        Timestamp rowKey = convertKeyToSQLTimestamp(keyFactory.createRowKey(event), KeyFactory.ROW_KEY_PATTERN);
        String sid = keyFactory.createSid(event);
        String eventHash = keyFactory.createEventHash(event);
        JPAEventEntity entity = new JPAEventEntity(partitionKey, rowKey, sid, eventHash, event.encode());
        return entity;
    }

    /**
     * @param key row- or partition-key as string (may include SID suffix, which is ignored)
     * @param formatPattern pattern used to format a row- or partition-key (use {@link KeyFactory#ROW_KEY_PATTERN} or
     * {@link KeyFactory#PARTITION_KEY_PATTERN})
     * @return SQL timestamp representation of the key
     */
    public Timestamp convertKeyToSQLTimestamp(String key, String formatPattern) {
        String timeString = key.substring(0, formatPattern.length());
        //Workaround for a java 8 bug - https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8031085
        if(formatPattern.endsWith("SSS")){
            timeString = addSeparatorForFraction(timeString);
            formatPattern = addSeparatorForFraction(formatPattern);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern).withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timeString,formatter);
        return new Timestamp(zonedDateTime.toInstant().toEpochMilli());
    }

    private String addSeparatorForFraction(String timeString) {
        StringBuilder timeStringBuilder = new StringBuilder(timeString);
        timeStringBuilder.insert(timeString.length() - 3, ".");
        timeString = timeStringBuilder.toString();
        return timeString;
    }
}
