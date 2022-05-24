package de.urbanpulse.dist.util;

import de.urbanpulse.dist.outbound.MainVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class StatementConsumerManagementVerticle extends AbstractVerticle {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Map which keeps track of the registered consumers. In most cases you will only read from this Map und any subclass because
     * the editing is done in {@link StatementConsumerManagementVerticle}.
     */
    protected final Map<String, MessageConsumer> statementToConsumerMap = new HashMap<>();
    /**
     * Map which keeps track of the update listeners which are registered for each statement.
     * In most cases you will only read from this Map und any subclass because
     * the editing is done in {@link StatementConsumerManagementVerticle}.
     */
    protected final Map<String, Set<UpdateListenerConfig>> statementToListenerMap = new HashMap<>();

    /**
     * Set this variable in your subclass. It determines the event bus path the consumer listen for.
     */
    protected String statementPrefix = MainVerticle.LOCAL_STATEMENT_PREFIX;

    private final MessageConsumerUnregistrationHelper unregistrationHelper = new MessageConsumerUnregistrationHelper();


    /**
     * Method which is called whenever an event is received. It is only called for statements for which
     * update listeners have been registered.
     *
     * @param statementName the name of the statement the event belongs to.
     * @param event         the event.
     */
    protected abstract void handleEvent(String statementName, JsonObject event);

    private void handleEventMessage(Message<JsonObject> message) {
        JsonObject event = message.body();
        String statementName = event.getString("statementName");
        handleEvent(statementName, event);
    }

    /**
     * Registers a consumer which is handled by {@link StatementConsumerManagementVerticle#setupConsumer(Message)}
     * with the given address. Call this method in the start method of your verticle.
     *
     * @param setupConsumerAddress the address where the consumer is registered.
     */
    protected void registerSetupConsumer(String setupConsumerAddress) {
        vertx.eventBus().localConsumer(setupConsumerAddress, this::setupConsumer);
    }

    private void setupConsumer(Message<JsonObject> message) {
        JsonObject setup = message.body();
        logger.info("Command received: {0}", setup.encode());
        if (setup.containsKey("register")) {
            registerUpdateListener(message);
        } else if (setup.containsKey("unregister")) {
            unregisterUpdateListener(message);
        } else if (setup.containsKey("reset")) {
            reset(message);
        } else {
            logger.error("Invalid command received!");
            message.fail(404, "Invalid command: " + setup.encode());
        }
    }

    private void registerUpdateListener(Message<JsonObject> message) {
        JsonObject jsonConfig = message.body().getJsonObject("register");
        UpdateListenerConfig ulConfig = new UpdateListenerConfig(jsonConfig);
        logger.debug("Listener config received: {0}", ulConfig);

        if (!ulConfig.isValid()) {
            logger.error("Given config is not valid: " + ulConfig);
            message.fail(400, "Received config without id, target or statement name: " + ulConfig);
            return;
        }

        Promise<Void> subclassRegisterPromise = Promise.promise();


        try {
            registerUpdateListener(ulConfig, subclassRegisterPromise);
        } catch (RuntimeException e) {
            // handle exceptions gracefully
            logger.error("Error during registration of update listener with config {0}", e, ulConfig);
            subclassRegisterPromise.tryFail(e);
        }

        subclassRegisterPromise.future().onComplete(asyncResult -> {
            if (asyncResult.succeeded()) {
                // check if there exists an consumer for this statement
                final String statementName = ulConfig.getStatementName();
                if (!statementToConsumerMap.containsKey(statementName)) {
                    logger.info("Registering eventbus consumer for statement: {0}", statementName);
                    final String address = statementPrefix + statementName;
                    MessageConsumer consumer = vertx.eventBus().localConsumer(address, this::handleEventMessage);
                    statementToConsumerMap.put(statementName, consumer);
                }
                statementToListenerMap.computeIfAbsent(statementName, c -> new HashSet<>()).add(ulConfig);
                logger.debug("Registered listener with config {0} successfully", jsonConfig.encode());
                message.reply(new JsonObject().put("status", "register command executed"));
            } else {
                message.fail(500, asyncResult.cause().getMessage());
            }
        });
    }

    private void unregisterUpdateListener(Message<JsonObject> message) {
        JsonObject jsonConfig = message.body().getJsonObject("unregister");
        UpdateListenerConfig ulConfig = new UpdateListenerConfig(jsonConfig);
        logger.debug("Listener config received: {0}", ulConfig);

        if (!ulConfig.isValid()) {
            logger.error("Given config is not valid: " + ulConfig);
            message.fail(400, "Received config without id, target or statement name: " + ulConfig);
            return;
        }

        final String statementName = ulConfig.getStatementName();
        if (!statementToListenerMap.containsKey(statementName) || !statementToListenerMap.get(statementName).contains(ulConfig)) {
            logger.error("Got request to unregister listener which was never registered");
            message.fail(404, "The given update listener " + ulConfig + " was never registered");
            return;
        }

        Promise<Void> subclassUnregisterFuture = Promise.promise();

        try {
            unregisterUpdateListener(ulConfig, subclassUnregisterFuture);
        } catch (RuntimeException e) {
            // handle exceptions gracefully
            logger.error("Error during unregistration of update listener with config {0}", e, ulConfig);
            subclassUnregisterFuture.tryFail(e);
        }


        subclassUnregisterFuture.future().onComplete(asyncResult -> {
            if (asyncResult.succeeded()) {
                statementToListenerMap.get(statementName).remove(ulConfig);
                if (statementToListenerMap.get(statementName).isEmpty()) {
                    statementToListenerMap.remove(statementName);
                    MessageConsumer consumerToUnregister = statementToConsumerMap.remove(statementName);
                    consumerToUnregister.unregister();
                    logger.info("Unregistered consumer for statement " + statementName);
                }

                logger.info("Unregistered listener with config {0} successfully", jsonConfig.encode());
                message.reply(new JsonObject().put("status", "unregister command executed"));
            } else {
                message.fail(500, asyncResult.cause().getMessage());
            }
        });
    }

    private void reset(Message<JsonObject> message) {
        logger.debug("Reset command received. Resetting...");

        Promise<Void> subclassResetDone = Promise.promise();
        Promise<Void> consumerUnregisteredFuture = Promise.promise();

        try {
            reset(subclassResetDone);
        } catch (RuntimeException e) {
            // handle exceptions gracefully
            logger.error("Error during resetting", e);
            subclassResetDone.tryFail(e);
        }

        subclassResetDone.future().onComplete(asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Reset failed partially. Trying to unregister consumers anyway", asyncResult.cause());
            }
            // unregister consumers either way because this is most likely the biggest part of the reset.
            logger.debug("Unregistering all consumers");
            unregistrationHelper.unregister(statementToConsumerMap.values(), consumerUnregisteredFuture);
        });

        consumerUnregisteredFuture.future().onComplete(asyncResult -> {
            statementToConsumerMap.clear();
            statementToListenerMap.clear();
            logger.info("Completed reset.");
            message.reply(new JsonObject().put("status", "register command executed"));
        });
    }


    /**
     * This method is called whenever a request to register a update listener was received.
     * You have to complete the future when your pre-register work has been done.
     * If something went wrong during your registration fail the future.
     * This will lead to a failing the message request of registering the update listener. In this case no new consumer will be
     * registered.
     *
     * @param ulConfig        the config of the update listener which shall be registered.
     * @param completedPromise the promise which has to be completed when your pre-register work has been done
     */
    protected void registerUpdateListener(UpdateListenerConfig ulConfig, Promise<Void> completedPromise) {
        registerUpdateListener(ulConfig);
        completedPromise.complete(null);
    }

    /**
     * This method is called whenever a request to unregister a consumer was received. You have to complete the future when your
     * pre-unregister work has been completed.
     * It is guarantied that the given  update listener has been registered before.
     * If something went wrong during your unregistration fail the future.
     * This will lead to a failing the message request not unregistering the given update listener
     *
     * @param ulConfig        the config of the update listener which shall be registered.
     * @param completedPromise the promise has to be completed when your pre-unregister work has been done.
     */
    protected void unregisterUpdateListener(UpdateListenerConfig ulConfig, Promise<Void> completedPromise) {
        unregisterUpdateListener(ulConfig);
        completedPromise.complete(null);
    }

    /**
     * This method is called whenever a request to reset was received. Reset any values your subclass has to reset and complete the
     * future afterwards. Don't unregister any consumers yourself because this is handled by this class.
     *
     * @param completedPromise
     */
    protected void reset(Promise<Void> completedPromise) {
        reset();
        completedPromise.complete();
    }


    /**
     * The synchronous version of {@link StatementConsumerManagementVerticle#registerUpdateListener(UpdateListenerConfig, Promise)}.
     * Thrown exceptions are automatically mapped to failed futures.
     *
     * @param ulConfig he config of the update listener which shall be registered.
     */
    protected void registerUpdateListener(UpdateListenerConfig ulConfig) {
    }

    /**
     * The synchronous version of {@link StatementConsumerManagementVerticle#unregisterUpdateListener(UpdateListenerConfig, Promise)}.
     * Thrown exceptions are automatically mapped to failed futures.
     *
     * @param ulConfig he config of the update listener which shall be unregistered.
     */
    protected void unregisterUpdateListener(UpdateListenerConfig ulConfig) {
    }

    /**
     * The synchronous version of {@link StatementConsumerManagementVerticle#reset(Future)}
     */
    protected void reset() {
    }

    /**
     * Returns a Map which maps each statement to a registered consumer for this statement. The returned Map is copy of the
     * internally used Map
     *
     * @return a <b>copy</b> of the registered statement consumers
     */
    public Map<String, MessageConsumer> getStatementToConsumerMap() {
        return new HashMap<>(statementToConsumerMap);
    }

    /**
     * Returns a map which maps each statement its registered update listeners. The returned Map is copy of the
     * internally used Map.
     *
     * @return a <b>copy</b> of the registered update listeners
     */
    public Map<String, Set<UpdateListenerConfig>> getStatementToListenerMap() {
        return new HashMap<>(statementToListenerMap);
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        unregistrationHelper.unregister(statementToConsumerMap.values(), r -> {
            statementToConsumerMap.clear();
            statementToListenerMap.clear();
            stopPromise.complete();
        });
    }
}
