package de.urbanpulse.transfer;

/**
 * A listener that is notified when messages are lost (detected by messageSN out of order)
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@FunctionalInterface
public interface MessageLossListener {
    void messageLost();
}
