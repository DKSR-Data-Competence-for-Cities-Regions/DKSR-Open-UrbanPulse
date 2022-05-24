package city.ui.shared.commons.time;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


/**
 * A common format for timestamp is used in several places. This class offers a
 * common format.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPDateTimeFormat {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
    //For Joda-Time we need to use Z at the end
    private static final String DEPRECATED_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    //Parsing and Formatting will set ZoneId to "Z"
    private static final DateTimeFormatter DATETIME_FORMATTER_WITH_ZONE_Z = DATE_TIME_FORMATTER.withZone(ZoneId.of("Z"));

    private static final org.joda.time.format.DateTimeFormatter DEPRECATED_DATETIME_FORMATTER;


    static {
        //check if joda time exists in the environment. If not don't initialize the deprecated formatter.
        org.joda.time.format.DateTimeFormatter deprecatedFormatter;
        try {
            Class.forName("org.joda.time.format.DateTimeFormatter");
            deprecatedFormatter = org.joda.time.format.DateTimeFormat.forPattern(DEPRECATED_DATETIME_PATTERN);
        } catch (ClassNotFoundException e) {
            deprecatedFormatter = null;
        }
        DEPRECATED_DATETIME_FORMATTER = deprecatedFormatter;
    }

	private UPDateTimeFormat(){}

    /**
     * This method returns a DateTimeFormatter for the Pattern "yyyy-MM-dd'T'HH:mm:ss.SSSX".
     * Using this pattern to format a ZonedDateTime will automatically convert it to UTC. The result of a format call will always
     * have the timezone UTC.
     * Using this formatter to parse a timestamp string the timezone will be ignored and replaced by UTC. The timestamp then may
     * differ from the original time.
     *
     * @return a formatter for the Pattern "yyyy-MM-dd'T'HH:mm:ss.SSSX"
     */
    public static DateTimeFormatter getFormatterWithZoneZ() {
        return DATETIME_FORMATTER_WITH_ZONE_Z;
    }

    /**
     * This method returns a DateTimeFormatter for the Pattern "yyyy-MM-dd'T'HH:mm:ss.SSSX".
     * Using this pattern to format a ZonedDateTime will keep its timezone. The result of a format call will not always be UTC.
     * Using this formatter to parse a timestamp string the timezone will be used. The timestamp then represents the original time.
     *
     * @return a formatter for the Pattern "yyyy-MM-dd'T'HH:mm:ss.SSSX"
     */
    public static DateTimeFormatter getFormatter() {
        return DATE_TIME_FORMATTER;
    }

    /**
     * @return the pattern that is used for the DateTimeFormatter
     */
    public static String getDateTimePattern() {
        return DATETIME_PATTERN;
    }

    /**
     * @return a datetimeformatter that should not be used
     * @deprecated the website of the joda-time project states, that it will not
     * be continued for java8 as the ideas of joda-time are now covered by "JSR
     * 310: Date and Time API"
     * <p>
     * Also if you use this method you have to supply a dependency to joda time yourself because this module does not include it.
     */
    @Deprecated
    public static org.joda.time.format.DateTimeFormatter getFormatterr() {
        return DEPRECATED_DATETIME_FORMATTER;
    }
}
