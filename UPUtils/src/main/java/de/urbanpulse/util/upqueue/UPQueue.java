package de.urbanpulse.util.upqueue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface UPQueue<E> {

    void addMessage(E message);

    void flush();

    void close();

}
