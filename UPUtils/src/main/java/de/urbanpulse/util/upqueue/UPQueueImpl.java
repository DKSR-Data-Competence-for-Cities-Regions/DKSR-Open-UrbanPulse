package de.urbanpulse.util.upqueue;

import java.util.ArrayList;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPQueueImpl<E> implements UPQueue<E> {

    private final int workerCount;
    private final List<UPQueueWorker<E>> worker = new ArrayList<>();
    private int selector = 0;
    private final UPQueueHandler handler;

    public UPQueueImpl(final UPQueueWorkerFactory<E> workerFactory, final UPQueueHandler handler, final int workerCount, final int batchSize, final int queueCapacity) {
        this.handler = handler;
        this.workerCount = workerCount;
        for (int i = 0; i < workerCount; i++) {
            worker.add(workerFactory.createWorker(batchSize, queueCapacity, handler));
        }
    }

    public UPQueueImpl(final UPQueueWorkerFactory<E> workerFactory, final UPQueueHandler handler, final int workerCount, final int batchSize) {
        this.workerCount = workerCount;
        this.handler = handler;
        for (int i = 0; i < workerCount; i++) {
            worker.add(workerFactory.createWorker(batchSize, handler));
        }
    }

    @Override
    public synchronized void addMessage(E message) {
        worker.get(selector++ % workerCount).addMessage(message);
    }

    @Override
    public void flush() {
        for (UPQueueWorker<E> singleWorker : worker) {
            singleWorker.flush();
        }
    }

    @Override
    public void close() {
        flush();
        for (UPQueueWorker<E> singleWorker : worker) {
            singleWorker.stop();
        }
        handler.close();
    }

}
