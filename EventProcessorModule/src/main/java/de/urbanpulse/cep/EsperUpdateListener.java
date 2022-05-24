package de.urbanpulse.cep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.util.upqueue.CEPQueueWorkerFactory;
import de.urbanpulse.util.upqueue.UPQueue;
import de.urbanpulse.util.upqueue.UPQueueHandler;
import de.urbanpulse.util.upqueue.UPQueueImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EsperUpdateListener implements UpdateListener {

    private final static Logger LOG = LoggerFactory.getLogger(EsperUpdateListener.class);

    private static final int QUEUE_CAPACITY = 100000;
    private static final int BATCH_SIZE = 2000;
    private static final int WORKER_COUNT = 2;

    private final UPQueue<JsonArray> upQueue;
    private final String listenerId;
    private final String statement;
    private final String outboundDestination;

    private final EventBeanConverter eventBeanConverter = new EventBeanConverter();

    public EsperUpdateListener(String listenerId, String statement, String outboundDestination, MessageProducer messageProducer) {
        this.listenerId = listenerId;
        this.statement = statement;
        this.outboundDestination = outboundDestination;
        this.upQueue = new UPQueueImpl<>(new CEPQueueWorkerFactory<>(), new UPQueueHandler<JsonArray>() {
            public void handle(List<JsonArray> objects) {
                if (objects.isEmpty()) {
                    LOG.info("Empty object, returning");
                    return;
                }
                JsonObject dataObject = new JsonObject();
                JsonArray dataArray = new JsonArray();
                for (JsonArray jsonArray : objects){
                   dataArray.addAll(jsonArray);
                }
                dataObject.put("data", dataArray);

                messageProducer.send(outboundDestination, dataObject.toBuffer(), null);
                LOG.debug("UpdateListener (" + listenerId + "): Message sent");
            }

            public void close() {
                LOG.info("Close called");
            }
        }, WORKER_COUNT, BATCH_SIZE, QUEUE_CAPACITY);
    }

    String getId() {
        LOG.info("Returning id: " + listenerId);
        return listenerId;
    }

    String getStatement() {
        LOG.info("Returning statement: " + statement);
        return statement;
    }

    String getOutboundDestination() {
        LOG.info("Returning outbound destination: " + outboundDestination);
        return outboundDestination;
    }

    @Override
    public void update(EventBean[] ebs, EventBean[] ebs1) {
        JsonArray eventsJson = eventBeanConverter.toJsonArray(ebs, statement);
        LOG.debug("Adding eventsJson to queue");
        upQueue.addMessage(eventsJson);
    }

}
