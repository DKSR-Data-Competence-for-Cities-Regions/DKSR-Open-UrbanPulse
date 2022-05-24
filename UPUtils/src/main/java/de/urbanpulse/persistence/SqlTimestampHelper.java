package de.urbanpulse.persistence;

import java.sql.Timestamp;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SqlTimestampHelper {

    public static Timestamp convertToTimestamp(String dateWithOptionalSuffix, String formatPattern) {
        String timeString = dateWithOptionalSuffix.substring(0, formatPattern.length());
        DateTimeFormatter formatter = DateTimeFormat.forPattern(formatPattern);
        DateTime jodaDateTime = formatter.parseDateTime(timeString);
        return new Timestamp(jodaDateTime.getMillis());
    }

}
