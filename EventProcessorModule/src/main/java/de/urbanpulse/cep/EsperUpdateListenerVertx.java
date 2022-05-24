package de.urbanpulse.cep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.eventbus.vertx.VertxMessageProducer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EsperUpdateListenerVertx implements UpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(EsperUpdateListenerVertx.class);

    private final Vertx vertx;
    private final String statementName;
    private JsonArray targets;
    private final EventBeanConverter eventBeanConverter;
    private MessageProducer messageProducer;

    public EsperUpdateListenerVertx(Vertx vertx, EventBeanConverter eventBeanConverter, MessageProducer messageProducer,
                                    String statementName, JsonArray targets) {
        this.vertx = vertx;
        this.statementName = statementName;
        this.targets = targets;
        this.eventBeanConverter = eventBeanConverter;
        this.messageProducer = messageProducer;
    }

    public EsperUpdateListenerVertx(Vertx vertx, JsonObject config, MessageProducer messageProducer) {
        this(vertx, new EventBeanConverter(), messageProducer, config.getString("name"),
                config.getJsonArray(CEPCommandHandler.KEY_TARGETS));
    }

    public EsperUpdateListenerVertx(Vertx vertx, JsonObject config) {
        this(vertx, new EventBeanConverter(), new VertxMessageProducer(vertx),
                config.getString("name"), config.getJsonArray(CEPCommandHandler.KEY_TARGETS));
    }

    public void setTargets(JsonArray targets){
        this.targets = targets;
    }

    public JsonArray getTargets() {
        return targets.copy();
    }

    @Override
    public void update(EventBean[] ebs, EventBean[] ebs1) {
        JsonArray eventsJson = eventBeanConverter.toJsonArray(ebs, statementName);

        if (eventsJson.isEmpty()) {
            LOG.info("Empty object, returning");
            return;
        }
        JsonArray data = new JsonArray();
        eventsJson.stream()
                .map(JsonObject.class::cast)
                .forEach(data::add);
        vertx.runOnContext(context ->
                targets.stream().map(String.class::cast).forEach(target -> {
                    messageProducer.send(target, data.toBuffer(), null);
                    LOG.debug("UpdateListener (" + target + "): Message sent");
                })
        );
    }

}
