package de.urbanpulse.util.upqueue;


import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MPSLogger {

    private Date start;
    private long messages;

    public MPSLogger() {
        this.messages = 0;
    }

    public void inc(int inc) {
        messages += inc;

        if (start == null) {
            start = new Date();
        } else {
            Long seconds;
            if (messages % 1000 == 0) {
                seconds = (new Date().getTime() - start.getTime());
                if (seconds > 0) {
                    Logger.getLogger(MPSLogger.class.getName()).log(Level.INFO, "M/msec " + messages / seconds);
                    System.out.println("M/sec " + messages / seconds);
                }
            }

        }
    }
}
