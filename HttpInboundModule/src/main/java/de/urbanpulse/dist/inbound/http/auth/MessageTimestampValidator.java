package de.urbanpulse.dist.inbound.http.auth;

import city.ui.shared.commons.time.UPDateTimeFormat;
import org.joda.time.*;


/**
 * checks if the "UrbanPulse-Timestamp" header is in valid range
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class MessageTimestampValidator {

    private static final int MAX_DEVIATION = 15;

    /**
     * <p>checks if the "UrbanPulse-Timestamp" header is in valid range (i.e. within 15 minutes of now)
     * this is thread-safe </p>
     * @param urbanPulseTimestamp UTC timestamp in ISO format "yyyyMMdd'T'HHmmss.SSSZ"
     * @return true if given timestamp is within 15 minutes of current time, false otherwise
     */
    public boolean isValid(String urbanPulseTimestamp) {
        try {
            DateTime dateTime = UPDateTimeFormat.getFormatterr().parseDateTime(urbanPulseTimestamp);
            DateTime now = DateTime.now(DateTimeZone.UTC);
            DateTime oldestValidTime = now.minusMinutes(MAX_DEVIATION);
            DateTime newestValidTime = now.plusMinutes(MAX_DEVIATION);
            return !dateTime.isBefore(oldestValidTime) && !dateTime.isAfter(newestValidTime);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * <p>checks if the "UrbanPulse-Timestamp" header is in invalid range (i.e. outside 15 minutes of now)
     * this is thread-safe </p>
     * @param urbanPulseTimestamp UTC timestamp in ISO format "yyyyMMdd'T'HHmmss.SSSZ"
     * @return false if given timestamp is within 15 minutes of current time, true otherwise
     */
    public boolean isInvalid(String urbanPulseTimestamp) {
        return !isValid(urbanPulseTimestamp);
    }
}
