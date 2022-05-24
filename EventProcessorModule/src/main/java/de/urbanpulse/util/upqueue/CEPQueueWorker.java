package de.urbanpulse.util.upqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CEPQueueWorker<E> implements UPQueueWorker<E> {

    private final LinkedBlockingQueue<E> queue;
    private final int batchSize;
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private final UPQueueHandler handler;
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    private final MPSLogger logger = new MPSLogger();

    public CEPQueueWorker(UPQueueHandler<E> handler) {
        this(1, Integer.MAX_VALUE, handler);
    }

    public CEPQueueWorker(int batchSize, UPQueueHandler<E> handler) {
        this(batchSize, 2 * batchSize, handler);
    }

    public CEPQueueWorker(int batchSize, int queueCapacity, UPQueueHandler<E> handler) {
        this.batchSize = batchSize;
        this.handler = handler;
        final int capacity = Math.max(batchSize, queueCapacity);
        this.queue = new LinkedBlockingQueue<>(capacity);
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (!flushing.get()) {
                    if (queue.size() > 0) {
                        flushing.set(true);
                        new Thread(new Publisher(handler, queue)).start();
                    }
                }
            }
        }, 100, 100);

    }

    private void checkQueue() {
        if (!flushing.get()) {
            if (this.queue.size() > 1000) {
                flushing.set(true);
                new Thread(new Publisher(this.handler, this.queue)).start();
            }
        }
    }

    @Override
    public void addMessage(E message) {
        this.queue.add(message);
        messageCount.incrementAndGet();
        checkQueue();
    }

    @Override
    public void flush() {
    }

    @Override
    public void stop() {
    }

    class Publisher implements Runnable {

        private final UPQueueHandler<E> handler;
        private final LinkedBlockingQueue<E> queue;

        public Publisher(UPQueueHandler<E> handler, LinkedBlockingQueue<E> queue) {
            this.handler = handler;
            this.queue = queue;
        }

        @Override
        public void run() {
            List<E> result = new ArrayList<>(batchSize);
            this.queue.drainTo(result, batchSize);
            this.handler.handle(result);
            flushing.set(false);
            checkQueue();
        }

    }

}
