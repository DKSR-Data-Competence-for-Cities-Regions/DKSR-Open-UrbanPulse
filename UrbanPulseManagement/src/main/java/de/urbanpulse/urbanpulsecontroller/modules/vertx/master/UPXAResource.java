package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface UPXAResource {

    void start(String transactionId) throws UPXAException;

    void commit(String transactionId) throws UPXAException;

    void rollback(String transactionId) throws UPXAException;

}