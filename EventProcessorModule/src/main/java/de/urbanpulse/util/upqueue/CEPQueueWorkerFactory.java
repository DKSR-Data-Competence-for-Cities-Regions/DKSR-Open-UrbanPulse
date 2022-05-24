package de.urbanpulse.util.upqueue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CEPQueueWorkerFactory<E> implements UPQueueWorkerFactory<E> {

    @Override
    public UPQueueWorker<E> createWorker(UPQueueHandler<E> handler) {
        return new CEPQueueWorker<>(handler);
    }

    @Override
    public UPQueueWorker<E> createWorker(int batchSize, UPQueueHandler<E> handler) {
        return new CEPQueueWorker<>(batchSize, handler);
    }

    @Override
    public UPQueueWorker<E> createWorker(int batchSize, int queueCapacity, UPQueueHandler<E> handler) {
        return new CEPQueueWorker<>(batchSize, queueCapacity, handler);
    }

}
