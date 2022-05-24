package de.urbanpulse.transfer;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * This is a filter that returns true in the method {@link Filter#isLoggable(java.util.logging.LogRecord)} if
 * the LogRecord is a record for the access log.
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AccessLogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        return record.getLoggerName().startsWith("accessLog");
    }

}
