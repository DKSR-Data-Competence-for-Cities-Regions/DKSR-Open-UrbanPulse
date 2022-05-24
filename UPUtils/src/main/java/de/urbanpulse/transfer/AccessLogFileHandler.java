package de.urbanpulse.transfer;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AccessLogFileHandler extends FileHandler {
    public AccessLogFileHandler() throws IOException{
        super();
        setFilter(new AccessLogFilter());
        setLevel(Level.INFO);
    }
}
