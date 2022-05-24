package de.urbanpulse.dist.util.upqueue;

import de.urbanpulse.util.upqueue.UPQueueHandler;
import de.urbanpulse.util.upqueue.UPQueueWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundQueueWorker<E> implements UPQueueWorker<E> {

    private final LinkedBlockingQueue<E> queue;
    private final int batchSize;
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private final UPQueueHandler handler;
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private final ExecutorService executor;

    private Timer timer;

    public OutboundQueueWorker(UPQueueHandler handler) {
        this(1, Integer.MAX_VALUE, handler);
    }

    public OutboundQueueWorker(int batchSize, UPQueueHandler handler) {
        this(batchSize, 2 * batchSize, handler);
    }

    public OutboundQueueWorker(int batchSize, int queueCapacity, UPQueueHandler handler) {
        this.batchSize = batchSize;
        this.handler = handler;
        final int capacity = Math.max(batchSize, queueCapacity);
        queue = new LinkedBlockingQueue<>(capacity);
        this.executor = Executors.newSingleThreadExecutor();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkQueue(0);
            }
        }, 100, 100);

    }

    private void checkQueue(int maxQueueSize) {
        if (!flushing.get()) {
            if (queue.size() > maxQueueSize) {
                flushing.set(true);
                 executor.submit(new Publisher(handler, queue));
            }
        }
    }

    @Override
    public void addMessage(E message) {
        queue.add(message);
        messageCount.incrementAndGet();
        checkQueue(1000);
    }

    @Override
    public void flush() {
        checkQueue(0);
    }

    @Override
    public void stop() {
        timer.cancel();
        flush();
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
            queue.drainTo(result, batchSize);
            handler.handle(result);
            flushing.set(false);
            checkQueue(0);
        }

    }

}
