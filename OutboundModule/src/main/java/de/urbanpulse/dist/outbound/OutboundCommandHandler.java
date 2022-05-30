package de.urbanpulse.dist.outbound;

import de.urbanpulse.dist.outbound.client.HttpVerticle;
import de.urbanpulse.dist.outbound.server.ws.WsPublisherVerticle;
import de.urbanpulse.dist.outbound.server.ws.WsServerVerticle;
import de.urbanpulse.transfer.CommandArgsFactory;
import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.UndoCommand;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundCommandHandler extends CommandHandler {

    public static final String SETUP_ADDRESS = OutboundCommandHandler.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundCommandHandler.class);
    private final Map<String, String> listenerToVerticles = new HashMap<>();
    private final Map<String, JsonObject> clientListenerConfigMap = new HashMap<>();
    private final Map<String, JsonObject> serverListenerConfigMap = new HashMap<>();
    private final Map<String, List<String>> statementToListeners = new HashMap<>();
    private final CommandArgsFactory commandArgsFactory = new CommandArgsFactory();
    private final ErrorFactory errorFactory = new ErrorFactory();

    public OutboundCommandHandler(MainVerticle mainVerticle) {
        super(mainVerticle);
    }

    public void registerUpdateListener(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        registerUpdateListener(new JsonObject(args), createUndoCommand, callback);
    }

    public void registerUpdateListener(JsonObject args, boolean createUndoCommand, CommandResult callback) {
        deployVerticlesOrRegister(args, createUndoCommand, callback);
    }

    public void unregisterUpdateListener(Map<String, Object> args, boolean createUndoCommand, CommandResult callback) {
        undeployVerticlesOrUnregister(new JsonObject(args), createUndoCommand, callback);
    }

    private void deployVerticlesOrRegister(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback) {
        String target = listenerConfig.getString("target");
        String id = listenerConfig.getString("id");

        //check if update listener is already registered
        if (clientListenerConfigMap.containsKey(id) || serverListenerConfigMap.containsKey(id)) {
            callback.done(errorFactory.createErrorMessage("already registered listener with id: " + id), null);
        } else {
            try {
                target = cleanTargetOfEmptySpaces(target);
                String scheme = URI.create(target).getScheme().toLowerCase();
                //replace the old target so that future exceptions do not occur
                listenerConfig.put("target", target);
                registerOrDeployVerticleForURIScheme(listenerConfig, createUndoCommand, callback, scheme);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Unable to parse URI: " + target, e);
                callback.done(errorFactory.createErrorMessage("Unable to parse target URI: " + target), null);
            }
        }
    }

    protected String cleanTargetOfEmptySpaces(String target){
        if (target != null && !target.trim().isEmpty()) {
            //removes all whitespaces and non-visible characters (e.g., tab, \n).
            //otherwise targetUI does not have a scheme and an exception is raised
            return target.replaceAll("\\s+", "");
        } else {
            //we dont want to continue in the case we have a wrongly formatted URI
            throw new IllegalArgumentException("URL is empty or null!");
        }
    }

    private void registerOrDeployVerticleForURIScheme(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback,
                                                      String scheme){
        switch (scheme) {
            case "wss":
            case "ws":
                registerForWsServer(listenerConfig, createUndoCommand, callback);
                break;
            case "http":
            case "https":
                deployHttpVerticles(listenerConfig, createUndoCommand, callback);
                break;
            default:
                LOGGER.error("Unsupported Scheme: " + scheme);
                callback.done(errorFactory.createErrorMessage("Unsupported Scheme: " + scheme), null);
        }
    }



    private void registerForWsServer(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback) {
        JsonObject setup = new JsonObject();
        setup.put("register", listenerConfig);

        Promise<Message<Object>> wsServerVerticlePromise = Promise.promise();
        mainVerticle.getVertx().eventBus().request(WsServerVerticle.SETUP_ADDRESS, setup, wsServerVerticlePromise);

        Promise<Message<Object>> wsPublisherVerticlePromise = Promise.promise();
        mainVerticle.getVertx().eventBus().request(WsPublisherVerticle.SETUP_ADDRESS, setup, wsPublisherVerticlePromise);

        CompositeFuture.join(wsServerVerticlePromise.future(), wsPublisherVerticlePromise.future()).onComplete(result -> {
            if (result.succeeded()) {
                completeRegisterUpdateListener(listenerConfig, createUndoCommand, callback, serverListenerConfigMap);
            } else {
                callback.done(errorFactory.createErrorMessage(result.cause().getMessage()), null);
            }
        });
    }

    /**
     * Completes a update listener registration and creates an UndoCommand if necessary.
     *
     * @param listenerConfig    the config of the created update listener
     * @param createUndoCommand whether an UndoCommand shall be created and given to the callback
     * @param callback          callback whose done method will be called
     * @param configMap         the map where the updates listeners config will be stored
     */
    private void completeRegisterUpdateListener(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback,
            Map<String, JsonObject> configMap) {
        String id = listenerConfig.getString("id");
        String statementName = listenerConfig.getString("statementName");

        addListenerIdToStatementMap(id, statementName);
        configMap.put(id, listenerConfig);

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            undoCommand = new UndoCommand(LOGGER, this, "unregisterUpdateListener", commandArgsFactory.buildArgs("id", id));
        }
        callback.done(new JsonObject(), undoCommand);
    }

    /**
     * Completes the unregistration of an UpdateListener. It removes it's config from the given configMap and creates an UndoCommand
     * if requested.
     *
     * @param listenerConfig    the config of the listener which got removed. Must contain it's id
     * @param createUndoCommand whether an UndoCommand sholl be created, which would recreate the listener
     * @param callback          the callback whose done method will be called
     * @param configMap         the map from which the update listeners config will be removed
     */
    private void completeUnregisterUpdateListener(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback,
            Map<String, JsonObject> configMap) {
        String id = listenerConfig.getString("id");
        JsonObject removedListener = configMap.remove(id);

        String statementName = removedListener.getString("statementName");
        removeListenerFromStatementMap(id, statementName);

        UndoCommand undoCommand = null;
        if (createUndoCommand) {
            undoCommand = new UndoCommand(LOGGER, this, "registerUpdateListener", removedListener.getMap());
        }
        callback.done(new JsonObject(), undoCommand);

    }

    private void undeployVerticlesOrUnregister(JsonObject unregistrationConfig, boolean createUndoCommand, CommandResult callback) {
        String id = unregistrationConfig.getString("id");

        if (serverListenerConfigMap.containsKey(id)) {
            unregisterForWsServer(serverListenerConfigMap.get(id), createUndoCommand, callback);
        } else if (clientListenerConfigMap.containsKey(id)) {
            undeployHttpVerticles(clientListenerConfigMap.get(id), createUndoCommand, callback);
        } else {
            LOGGER.warn("Update Listener with id [" + id + "]is not registered. Skipping unregistration");
            callback.done(new JsonObject(), null);
        }
    }



    private void unregisterForWsServer(JsonObject unregistrationConfig, boolean createUndoCommand, CommandResult callback) {
        LOGGER.info("sending unregister to WS verticles...");
        JsonObject setup = new JsonObject();
        setup.put("unregister", unregistrationConfig);

        Promise<Message<Object>> wsServiceUnregisterPromise = Promise.promise();
        Promise<Message<Object>> wsPublisherUnregisterPromise = Promise.promise();

        final EventBus eventBus = mainVerticle.getVertx().eventBus();
        eventBus.request(WsServerVerticle.SETUP_ADDRESS, setup, wsServiceUnregisterPromise);
        eventBus.request(WsPublisherVerticle.SETUP_ADDRESS, setup, wsPublisherUnregisterPromise);
        LOGGER.info("sent unregister to WS verticles");

        CompositeFuture.join(wsPublisherUnregisterPromise.future(), wsServiceUnregisterPromise.future()).onComplete(result -> {
            if(result.succeeded()){
                completeUnregisterUpdateListener(unregistrationConfig, createUndoCommand, callback, serverListenerConfigMap);
            }else {
                callback.done(errorFactory.createErrorMessage(result.cause().getMessage()), null);
            }
        });
    }

    public void reset(CommandResult callback) {
        Promise<Void> httpResetFuture = Promise.promise();
        Promise<Void> wsResetFuture = Promise.promise();

        final ArrayList<String> allVerticles = new ArrayList<>(listenerToVerticles.values());
        resetHttpRelated(allVerticles, httpResetFuture);
        httpResetFuture.future().onComplete(voidVar -> resetWsServerRelated(wsResetFuture));
        wsResetFuture.future().onComplete(result -> {
            serverListenerConfigMap.clear();
            clientListenerConfigMap.clear();
            listenerToVerticles.clear();
            statementToListeners.clear();
            callback.done(new JsonObject(), null);
        });
    }



    private void resetHttpRelated(final List<String> allVerticles, Handler<AsyncResult<Void>> callback) {
        if (allVerticles.isEmpty()) {
            callback.handle(Future.succeededFuture(null));
            return;
        }

        String deploymentId = allVerticles.remove(0);
        mainVerticle.undeployVerticle(deploymentId, (AsyncResult<Void> result) -> {
            if (!result.succeeded()) {
                LOGGER.fatal("can't undeploy http verticle " + deploymentId, result.cause());
            }
            resetHttpRelated(allVerticles, callback);
        });
    }

    private void resetWsServerRelated(Handler<AsyncResult<Void>> callback) {
        JsonObject setup = new JsonObject();
        setup.put("reset", new JsonObject());

        mainVerticle.getVertx().eventBus().request(WsPublisherVerticle.SETUP_ADDRESS, setup,
                (AsyncResult<Message<JsonObject>> event1) -> mainVerticle.getVertx().eventBus()
                        .request(WsServerVerticle.SETUP_ADDRESS, setup,
                                (AsyncResult<Message<JsonObject>> event2) -> callback.handle(Future.succeededFuture(null))));
    }

    protected void deployHttpVerticles(final JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback) {
        if (mainVerticle.config().getBoolean("testNoUpdateListeners", false)) {
            LOGGER.info("ignoring to deploy http verticles with " + listenerConfig);
            return;
        }
        LOGGER.info("deploying http verticles with " + listenerConfig);
        final String updateListenerId = listenerConfig.getString("id");

        mainVerticle.deployWorkerVerticle(HttpVerticle.class.getName(), listenerConfig, 1, result -> {
            if (result.succeeded()) {
                String deploymentId1 = result.result();
                listenerToVerticles.put(updateListenerId, deploymentId1);
                completeRegisterUpdateListener(listenerConfig, createUndoCommand, callback, clientListenerConfigMap);
            } else {
                LOGGER.fatal("can't deploy " + HttpVerticle.class.getName(), result.cause());
                listenerToVerticles.remove(updateListenerId);
                callback.done(errorFactory.createErrorMessage("failed to register listener with id: " + updateListenerId), null);
            }
        });
    }

    private void addListenerIdToStatementMap(String updateListenerId, String statementName) {
        statementToListeners.computeIfAbsent(statementName, k -> new ArrayList<>()).add(updateListenerId);
    }

    private void removeListenerFromStatementMap(String listenerId, String statementName) {
        List<String> listeners = statementToListeners.get(statementName);
        listeners.remove(listenerId);
        if (listeners.isEmpty()) {
            statementToListeners.remove(statementName);
        }
    }

    private void undeployHttpVerticles(JsonObject listenerConfig, boolean createUndoCommand, CommandResult callback) {
        if (mainVerticle.config().getBoolean("testNoUpdateListeners", false)) {
            LOGGER.info("ignoring to deploy http verticles with " + listenerConfig);
            return;
        }
        LOGGER.info("undeploying http verticles with " + listenerConfig);
        final String updateListenerId = listenerConfig.getString("id");
        String deploymentId = listenerToVerticles.get(updateListenerId);
        if (deploymentId == null) {
            LOGGER.info("undeploying http verticles: unknown listener id " + updateListenerId);
            callback.done(errorFactory.createErrorMessage("unknown listener " + updateListenerId), null);
            return;
        }

        mainVerticle.undeployVerticle(deploymentId, (AsyncResult<Void> result) -> {
            if (result.succeeded()) {
                listenerToVerticles.remove(updateListenerId);
                completeUnregisterUpdateListener(listenerConfig, createUndoCommand, callback, clientListenerConfigMap);
            } else {
                LOGGER.fatal("can't undeploy all http verticles for listener " + updateListenerId, result.cause());
                callback.done(errorFactory.createErrorMessage("failed to unregister listener " + updateListenerId), null);
            }
        });
    }

    void setup(JsonObject config, CommandResult resultHandler) {
        // Initially set to 1 in case no update listener is to be registered
        // because the registerResultHandler will be called anyway and decrement
        // it to 0. Will be set to the correct value otherwise.

        AtomicInteger registrationCounter = new AtomicInteger(1);

        CommandResult registerResultHandler = (json, undoCmd) -> {
            if (registrationCounter.decrementAndGet() == 0) {
                resultHandler.done(null, null);
            }
        };

        this.reset((JsonObject result, UndoCommand cmd) -> {

            JsonArray listeners = config.getJsonArray("listeners");

            if (listeners == null || listeners.size() == 0) {
                registerResultHandler.done(null, null);
            } else {
                registrationCounter.set(listeners.size());

                listeners.stream().map(JsonObject.class::cast)
                        .forEach(listener -> registerUpdateListener(listener, false, registerResultHandler));
            }
        });
    }


    public List<String> getListeners(String statementName) {
        return statementToListeners.get(statementName);
    }

}
