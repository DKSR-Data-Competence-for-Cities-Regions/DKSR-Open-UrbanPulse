package de.urbanpulse.urbanpulsecontroller.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@LocalBean
public class PerfTestEndToEndLogger {

    private long remainingCEPEventCount;
    private long originalRemainingCEPEventCount;
    private boolean stopLogging;
    private boolean logUnexpected;

    @TransactionAttribute(NOT_SUPPORTED)
    public void onReceived() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, ">>>>>>> events JSON received");
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void setLogUnexpected(boolean logUnexpected) {
        this.logUnexpected = logUnexpected;
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void onExpectEvents(long expectedEventCount) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "<<< PERFLOG >>> expecting [{0}] CEP events", expectedEventCount);
        stopLogging = false;
        remainingCEPEventCount = originalRemainingCEPEventCount = expectedEventCount;
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void onCEPEvent() {
        remainingCEPEventCount--;

        if (logUnexpected) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "<<< PERFLOG >>> CEP events left: {0}", remainingCEPEventCount);
        }

        if (stopLogging) {
            return;
        }

        if (originalRemainingCEPEventCount == (remainingCEPEventCount-1)) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "<<< PERFLOG >>> done with first expected CEP event");
        }

        if (remainingCEPEventCount == 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "<<< PERFLOG >>> done with last expected CEP event");
            stopLogging = true;
        }
    }
}
