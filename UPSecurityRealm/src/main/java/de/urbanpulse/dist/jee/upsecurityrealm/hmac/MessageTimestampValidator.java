package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.exception.InvalidTimeStampException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * checks if the "UrbanPulse-Timestamp" header is in valid range
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class MessageTimestampValidator {

    private static final Logger LOG = Logger.getLogger(MessageTimestampValidator.class.getName());

    private static final int MAX_DEVIATION_IN_MINUTES = 15;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");


    /**
     * Validates timestamp value.
     * Timestamp is valid if not older than 15 minutes and not newer than 15 minutes compared to current time.
     *
     * @param urbanPulseTimestamp value of "UrbanPulse-Timestamp" header as string
     * @throws InvalidTimeStampException is thrown in case of invalid header or error during validation
     */
    public static void validateTimeStamp(String urbanPulseTimestamp) throws InvalidTimeStampException {
        LOG.fine("Validating timestamp...");

            ZonedDateTime dateTime = ZonedDateTime.parse(urbanPulseTimestamp, ISO_FORMATTER);
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime oldestValidTime = now.minusMinutes(MAX_DEVIATION_IN_MINUTES);
            ZonedDateTime newestValidTime = now.plusMinutes(MAX_DEVIATION_IN_MINUTES);
            if (dateTime.isBefore(oldestValidTime) || dateTime.isAfter(newestValidTime)) {
                throw new InvalidTimeStampException("Timestamp is invalid");
            }
    }

}
