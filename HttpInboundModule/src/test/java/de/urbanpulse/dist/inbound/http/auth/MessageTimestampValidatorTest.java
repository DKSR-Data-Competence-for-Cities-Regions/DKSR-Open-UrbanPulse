package de.urbanpulse.dist.inbound.http.auth;

import io.vertx.core.logging.*;
import java.text.SimpleDateFormat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MessageTimestampValidatorTest {
  MessageTimestampValidator validator;
  @Before
  public void setup(){
    validator = new MessageTimestampValidator();
  }
  private Logger LOGGER = LoggerFactory.getLogger(MessageTimestampValidatorTest.class);

  @Test
  public void testIsValid(){
    DateTime now = DateTime.now(DateTimeZone.UTC);
    DateTime inTheRange = now.minusMinutes(14);
    DateTime tooSooner = now.minusMinutes(20);
    DateTime tooLate = now.plusMinutes(20);
    SimpleDateFormat wishedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    // Format the date to Strings
    String nowFormat = wishedFormat.format(now.toDate());
    String rangeFormat = wishedFormat.format(inTheRange.toDate());
    String soonerFormat = wishedFormat.format(tooSooner.toDate());
    String lateFormat = wishedFormat.format(tooLate.toDate());

    assertTrue(validator.isValid(nowFormat));
    assertTrue(validator.isValid(rangeFormat));
    assertFalse(validator.isValid(soonerFormat));
    assertFalse(validator.isValid(lateFormat));
  }
}
