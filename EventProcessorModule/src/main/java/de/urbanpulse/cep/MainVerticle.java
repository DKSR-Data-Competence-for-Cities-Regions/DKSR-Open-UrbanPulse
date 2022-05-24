package de.urbanpulse.cep;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import de.urbanpulse.eventbus.EventbusFactory;
import de.urbanpulse.eventbus.MessageProducer;
import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MainVerticle extends AbstractMainVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    private final DateTimeFormatter ISO_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static final String ESPER_IN = "ESPER_IN";
    private CEPCommandHandler cepCommandHandler;
    private EPServiceProvider esper;
    private MessageProducer messageProducer;
    private Counter totalEventsReceivedCounter;

    @Override
    protected CommandHandler createCommandHandler() {
        LOGGER.info("Creating CEP command handler");
        boolean logEventBusContent = config().getBoolean("logEventBusContent", false);

        cepCommandHandler = new CEPCommandHandler(this, messageProducer, esper);

        return cepCommandHandler;
    }

    @Override
    public void resetModule(Handler<Void> callback) {
        LOGGER.info("Resetting Event Processor Module");
        cepCommandHandler.reset(null, false, (JsonObject, UndoCommand) -> {
            callback.handle(null);
        });
    }

    @Override
    protected Map<String, Object> createRegisterModuleConfig() {
        LOGGER.info("Creating register configuration");
        Map<String, Object> args = new HashMap<>();
        args.put("moduleType", "EventProcessor");
        return args;
    }

    @Override
    public void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
        LOGGER.info("Setting up Event Processor Module.");
        boolean setupResult = cepCommandHandler.setup(setup);
        if (setupResult) {
            setupResultHandler.handle(Future.succeededFuture());
        } else {
            setupResultHandler.handle(Future.failedFuture("CEP setup failed."));
        }
    }

    @Override
    public void start(Promise<Void> startPromise) {
        if (config() != null) {
            LOGGER.info("Starting CEP MainVerticle with this configuration: " + config());
        }

        try {
            getClass().getClassLoader().loadClass("com.espertech.esper.client.UpdateListener");
        } catch (ClassNotFoundException ex) {
            startPromise.fail("CEP dependency is missing!");
            return;
        }

        esper = EPServiceProviderManager.getProvider("myEsper");
        final long startDelay = config().getLong("startDelay", 1000L);

        JsonObject eventBusConfig = config().getJsonObject("eventBusImplementation");
        EventbusFactory eventbusFactory = EventbusFactory.createFactory(vertx, eventBusConfig);

        LOGGER.info("Waiting " + startDelay + " msec before deployment of verticles...");
        vertx.setTimer(startDelay, ignored -> {
            vertx.eventBus().localConsumer(ESPER_IN, (Message<JsonArray> message) -> {
                sendToEsper(message.body());
            });

            int instances = config().getInteger("cepReceiverVerticleInstances");
            Promise<MessageProducer> producerPromise = Promise.promise();
            Promise<String> deploymentPromise = Promise.promise();

            eventbusFactory.createMessageProducer(producerPromise);
            deployVerticle(CEPEventReceiver.class.getName(), config(), instances, deploymentPromise);

            CompositeFuture.all(producerPromise.future(), deploymentPromise.future())
                    .onComplete(hndlr -> {
                        if (hndlr.succeeded()) {
                            messageProducer = hndlr.result().resultAt(0);
                            startPromise.complete();
                            super.start();
                        } else {
                            LOGGER.error(hndlr.cause());
                            startPromise.fail(hndlr.cause());
                        }
                    });
        });
        int logRateForSIDErrors = config().getInteger("logRateForSIDErrors");
        if (logRateForSIDErrors > 0) {
            vertx.setPeriodic(logRateForSIDErrors, (Long event) -> {
                LOGGER.info("********************* no event types #" + typelessEventCount);
                typelessEventCount = 0;
            });
        }
    }

    protected void registerMeters(MeterRegistry registry) {
        if (registry != null) {
            totalEventsReceivedCounter = Counter.builder("cep_events_received")
                    .description("Number of events received.")
                    .register(registry);
        }
    }

    protected void incTotalEventsCounter(double amount) {
        if (totalEventsReceivedCounter != null) {
            totalEventsReceivedCounter.increment(amount);
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping CEP MainVerticle");
        messageProducer.close();
        super.stop();
        esper.initialize();
    }

    @Override
    public void deployVerticle(String verticleName, JsonObject verticleConfig, int verticleInstances, Handler<AsyncResult<String>> handler) {
        LOGGER.info("Getting combined config for deploying verticle");
        JsonObject combinedConfig = verticleConfig.copy().put("clientConfig", getModuleConfig());
        LOGGER.info("Deploy verticle");
        super.deployVerticle(verticleName, combinedConfig, verticleInstances, handler);
    }

    private void sendToEsper(JsonArray events) {
        incTotalEventsCounter(events.size());
        for (Object event : events) {
            sendToEsper((JsonObject) event);
        }
    }

    int typelessEventCount = 0;

    private void sendToEsper(JsonObject event) {
        LOGGER.debug("Sending event to Esper");
        String eventTypeName = event.getJsonObject("_headers", new JsonObject()).getString("eventType");
        if (eventTypeName != null && "".equals(eventTypeName)) {
            LOGGER.info("Sensor event type is empty");
            typelessEventCount++;
        }

        final Map<String, Object> eventAsMap = generateEventMap(event, cepCommandHandler.getEventTypes().get(eventTypeName));
        if (eventAsMap == null) {
            LOGGER.warn("Received unknown eventtype with name '" + eventTypeName + "', dropping event.");
        } else {
            esper.getEPRuntime().sendEvent(eventAsMap, eventTypeName);
            LOGGER.debug("Sent to Esper");
        }
    }

    protected Map<String, Object> generateEventMap(JsonObject eventMessage, Map<String, Object> eventConfig) throws IllegalArgumentException {
        if (eventConfig == null) {
            return null;
        }
        Map<String, Object> eventMap = new HashMap<>();

        for (String eventParameter : eventMessage.fieldNames()) {
            String configuredType = (String) eventConfig.get(eventParameter);
            if (null == configuredType) {
                continue;
            }

            switch (configuredType) {
                case EventParamTypes.MAP:
                    Optional<JsonObject> optionalJsonObject = Optional.ofNullable(eventMessage.getJsonObject(eventParameter));
                    eventMap.put(eventParameter, optionalJsonObject.map(JsonObject::getMap).orElse(null));
                    break;
                case EventParamTypes.LIST:
                    Optional<JsonArray> optionalJsonArray = Optional.ofNullable(eventMessage.getJsonArray(eventParameter));
                    eventMap.put(eventParameter, optionalJsonArray.map(JsonArray::getList).orElse(null));
                    break;
                case EventParamTypes.DOUBLE:
                    eventMap.put(eventParameter, eventMessage.getDouble(eventParameter));
                    break;
                case EventParamTypes.LONG:
                    eventMap.put(eventParameter, eventMessage.getLong(eventParameter));
                    break;
                case EventParamTypes.STRING:
                    eventMap.put(eventParameter, eventMessage.getString(eventParameter));
                    break;
                case EventParamTypes.DATE:
                    String dateString = eventMessage.getString(eventParameter);
                    if (dateString != null) {
                        try {
                            eventMap.put(eventParameter, DateTime.parse(dateString, ISO_FORMATTER).toDate());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Error parsing value of {0}. Event: {1}", e, eventParameter, eventMessage.encode());
                        }
                    } else {
                        LOGGER.trace("datetime " + eventParameter + " is null");
                        eventMap.put(eventParameter, null);
                    }
                    break;
                case EventParamTypes.BOOLEAN:
                    eventMap.put(eventParameter, eventMessage.getBoolean(eventParameter));
                    break;
                default:
                    LOGGER.error("event contains unsupported parameter type [" + configuredType + "]");
                    break;
            }
        }

        return eventMap;
    }
}
