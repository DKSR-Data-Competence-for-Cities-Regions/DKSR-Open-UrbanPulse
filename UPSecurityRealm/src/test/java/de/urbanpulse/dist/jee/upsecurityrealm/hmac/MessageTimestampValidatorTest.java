package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.exception.InvalidTimeStampException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MessageTimestampValidatorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void validateTimestampShouldThrowExceptionForATimestampOlderThan15Minutes() {
        exception.expect(InvalidTimeStampException.class);
        exception.expectMessage("Timestamp is invalid");

        DateTime tooOld = DateTime.now(DateTimeZone.UTC).minusMinutes(15).minusSeconds(1);
        String tooOldString = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").print(tooOld);
        MessageTimestampValidator.validateTimeStamp(tooOldString);
    }

    @Test
    public void validateTimestampShouldThrowExceptionForATimestampNewerThan15Minutes() {
        exception.expect(InvalidTimeStampException.class);
        exception.expectMessage("Timestamp is invalid");

        DateTime tooNew = DateTime.now(DateTimeZone.UTC).plusMinutes(15).plusSeconds(1);
        String tooNewString = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").print(tooNew);
        MessageTimestampValidator.validateTimeStamp(tooNewString);
    }

    @Test
    public void validateTimestampDoesNotThrowExceptionForTimestampWithin15Minutes() {
        DateTime recent = DateTime.now(DateTimeZone.UTC).minusMinutes(14).minusSeconds(59);
        String recentString = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").print(recent);
        MessageTimestampValidator.validateTimeStamp(recentString);
    }
}
