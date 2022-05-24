package de.urbanpulse.transfer.vertx;

import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.ConnectionHandler;
import de.urbanpulse.transfer.TransactionManager;
import de.urbanpulse.transfer.TransferStructureFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static de.urbanpulse.transfer.ErrorFactory.ERROR_CODE_TAG;
import static de.urbanpulse.transfer.ErrorFactory.ERROR_MESSAGE_TAG;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_HEADER;
import de.urbanpulse.util.status.UPModuleState;
import de.urbanpulse.util.status.UPStatusUtils;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * base-class for main verticles of UrbanPulse vert.x modules that can be
 * configured via the UP setup master
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractMainVerticle extends AbstractVerticle {

    public static final int SM_TIMEOUT = 60000;
    public static final int SM_RETRY_TIME = 60000;
    public static final String MODULE_RESET = "module_reset";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMainVerticle.class);
    private static final long DEFAULT_HEARTBEAT = 20000;
    private static final int SETUP_RETRY_TIME_IN_MSEC = 2000;
    private static final int MAX_OPEN_CIRCUITS_BEFORE_RESET = 2;
    private static final String SM_ADDRESS = "sm_address";

    private final AtomicInteger openCircuitCounter = new AtomicInteger(0);
    private final AtomicBoolean isRegistering = new AtomicBoolean(false);

    private String id = null;
    private Long heartbeatTimerId = null;
    private Long resetTimerId = null;
    private ConnectionHandler connectionHandler;
    private JsonObject moduleConfig;
    private CircuitBreaker breaker;

    /**
     * Set up a module. If the provided setup causes problems it is expected
     * that an exception is thrown.
     *
     * @param setup a setup for this module
     * @param setupResultHandler to handle the setup result
     */
    protected abstract void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler);

    protected abstract Map<String, Object> createRegisterModuleConfig();

    protected abstract CommandHandler createCommandHandler();

    protected abstract void resetModule(Handler<Void> callback);

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        if (isEmptyConfig()) {
            startPromise.fail("Config is empty (or corrupted) and thus not valid");
            return;
        }

        super.start(startPromise);
    }

    private void initClusterWatch() {
        if (vertx instanceof VertxImpl) {
            ClusterManager clusterManager = ((VertxImpl) vertx).getClusterManager();
            if (clusterManager instanceof HazelcastClusterManager) {
                HazelcastClusterManager hazelcastClusterManager = (HazelcastClusterManager) clusterManager;
                hazelcastClusterManager.getHazelcastInstance().getLifecycleService().addLifecycleListener((LifecycleEvent event) -> {
                    if (event.getState() == LifecycleState.MERGED) {
                        // Recovery otherwise our module won't receive any messages from UPManagement anymore.
                        LOGGER.warn("Cluster has been merged. Resetting connection.");
                        vertx.runOnContext(ignored -> resetConnection());
                    }
                });
            } else {
                LOGGER.warn("Unable to watch hazelcast cluster. ClusterManager is not a HazelcastClusterManager.");
            }
        } else {
            LOGGER.warn("Unable to watch hazelcast cluster. Vertx is not a VertxImpl.");
        }
    }

    @Override
    public void start() {
        this.moduleConfig = config().getJsonObject("clientConfig");
        this.connectionHandler = new ConnectionHandler();



        boolean logEventBusContent = config().getBoolean("logEventBusContent", false);
        if (logEventBusContent) {
            LOGGER.warn("############# logEventBusContent is true - this will leak credentials for some modules - be careful! #############");
        }
        connectionHandler.setTransport(new TransportLayerVertx(vertx.eventBus(), logEventBusContent, this::resetConnection));
        connectionHandler.setTransactionManager(new TransactionManager());
        connectionHandler.setCommandHandler(createCommandHandler());
        vertx.eventBus().consumer(MODULE_RESET, (Message<Void> event) -> {
            LOGGER.info("Module received RESET command.");
            resetConnection();
        });
        LOGGER.info("Module started");

        final CircuitBreakerOptions circuitBreakerOptions = getCircuitBreakerOptions();
        breaker = CircuitBreaker.create("setup-circuit-breaker", vertx, circuitBreakerOptions)
                .openHandler(handler -> openCircuitCounter.incrementAndGet())
                .closeHandler(handler -> openCircuitCounter.set(0));

        registerAtServer();

        initClusterWatch();
    }

    protected CircuitBreakerOptions getCircuitBreakerOptions() {
        CircuitBreakerOptions circuitBreakerOptions;
        if (config().containsKey("circuitBreakerOptions")) {
            JsonObject options = config().getJsonObject("circuitBreakerOptions");
            LOGGER.info("Override circuitBreakerOptions: " + options);
            circuitBreakerOptions = new CircuitBreakerOptions(options);
        } else {
            circuitBreakerOptions = new CircuitBreakerOptions()
                    .setMaxFailures(5)
                    .setTimeout(2000)
                    .setFallbackOnFailure(false)
                    .setResetTimeout(10000);
        }
        // currently we do not use the breakers notifications but they cause duplicated Strings in
        // the heap. So we disable them for now. We may enable it again if vert.x fixes the issue
        // A bug for this has been created here
        // https://github.com/vert-x3/vertx-circuit-breaker/issues/49
        circuitBreakerOptions.setNotificationAddress(null);
        return circuitBreakerOptions;
    }

    public final void resetConnection() {
        if (!isRegistering.get()) {
            LOGGER.debug("Starting to reset connection.");
            cancelResetTimerIfPresent();
            if (heartbeatTimerId != null) {
                cancelTimer(heartbeatTimerId);
                heartbeatTimerId = null;
            }
            connectionHandler.reset();
            resetModule();
        } else {
            registerNewResetTimerIfNeeded();
            LOGGER.info("Postponing resetConnection, since registerAtServer is in progress. Retrying in max {0} sec.", SM_RETRY_TIME / 1000);
        }
    }

    public final void resetModule() {
        if (!isRegistering.get()) {
            this.openCircuitCounter.set(0);
            resetModule((Void event1) -> {
                registerAtServer();
            });
        } else {
            registerNewResetTimerIfNeeded();
            LOGGER.info("Skipping resetModule, since registerAtServer is in progress. Trying resetConnection in max {0} sec.", SM_RETRY_TIME / 1000);
        }
    }

    public final void exitProcess(Map<String, Object> args) {
        int statusCode = 0;
        try {
            if (args.containsKey("statusCode")) {
                statusCode = (int) args.get("statusCode");
            }
        } catch (RuntimeException e) {
            statusCode = Integer.MAX_VALUE;
        }
        LOGGER.error("closing vert.x");
        Vertx.currentContext().owner().close();
        System.err.println("exiting process with statusCode[" + statusCode + "]");
        System.exit(statusCode);
    }

    protected JsonObject getModuleConfig() {
        return moduleConfig;
    }

    /**
     * register at the server and request setup information from it
     */
    private void registerAtServer() {
        LOGGER.info("Start to register at server.");
        if (!isRegistering.get()) {
            isRegistering.getAndSet(true);
            LOGGER.debug("Set isRegistering true.");
            Map<String, Object> args = createRegisterModuleConfig();

            LOGGER.debug("Sending 'register' command");
            connectionHandler.sendCommand(SM_ADDRESS, "register", args, SM_TIMEOUT, (JsonObject message) -> {
                LOGGER.debug("Message content for 'register' command: " + message);

                if (!isMessageFaultless(message) || message.getString("id") == null) {
                    LOGGER.warn("Received erroneous response for 'register' command. Stopped registering. Retrying to register module in max {0} msec.", SM_RETRY_TIME);
                    stopRegistering();
                    registerNewResetTimerIfNeeded();
                } else {
                    LOGGER.debug("Initializing connection data");
                    id = message.getString("id");
                    connectionHandler.setConnectionId(id, (AsyncResult<Void> e) -> {
                        requestSetupFromServer();
                    });
                }
            });
        } else {
            LOGGER.warn("Skipping registering, since registerAtServer is in progress.");
        }
    }

    private boolean isMessageFaultless(JsonObject message) {
        if (message.containsKey(TAG_HEADER) && message.getJsonObject(TAG_HEADER).containsKey(ERROR_CODE_TAG)) {
            JsonObject body = message.getJsonObject(TAG_BODY);
            JsonObject header = message.getJsonObject(TAG_HEADER);
            logErrorDetails(header, body, "Error while requesting server: ");
            return false;
        } else {
            return true;
        }
    }

    private void requestSetupFromServer() {
        breaker.<JsonObject>execute(this::requestSetupFromUPManagement
        ).onComplete(this::handleSetupResponse);
    }

    private void requestSetupFromUPManagement(Promise<JsonObject> promise) {
        LOGGER.info("Requesting setup from server");
        Map<String, Object> args = new HashMap<>();
        args.put("id", id);

        LOGGER.debug("Sending 'sendSetup' command");
        connectionHandler.sendCommand(SM_ADDRESS, "sendSetup", args, SM_TIMEOUT, (JsonObject message) -> {
            LOGGER.info("Received message for 'sendSetup' command.");
            if (isMessageFaultless(message)) {
                promise.complete(message);
            } else {
                promise.fail("Error while requesting setup from server");
            }
        });
    }

    private void handleSetupResponse(AsyncResult<JsonObject> asyncResult) {
        if (asyncResult.failed()) {
            LOGGER.error("Module setup failed.");
            if (openCircuitCounter.get() < MAX_OPEN_CIRCUITS_BEFORE_RESET) {
                LOGGER.warn("Retrying to request setup from server.");
                vertx.setTimer(SETUP_RETRY_TIME_IN_MSEC, timerID -> requestSetupFromServer());
            } else {
                LOGGER.error("Max amount of allowed open circuits! Resetting module!");
                stopRegistering();
                resetConnection();
            }
        } else {
            handleSetupMessage(asyncResult.result()).onComplete(result -> {
                if (result.succeeded()) {
                    LOGGER.info("Module setup successful!");
                    stopRegistering();
                } else {
                    LOGGER.error("Module setup failed! Retrying in max {0} msec.", result.cause(), SM_RETRY_TIME);
                    stopRegistering();
                    registerNewResetTimerIfNeeded();
                }
            });
        }
    }

    private void stopRegistering() {
        LOGGER.info("Stopped registering.");
        isRegistering.getAndSet(false);
    }

    private Future<JsonObject> handleSetupMessage(JsonObject message) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject body = message.getJsonObject(TAG_BODY);
        setupModule(body, h -> {
            if (h.succeeded()) {
                LOGGER.debug("Starting heartbeats");
                long heartbeat = body.getLong("heartbeat", DEFAULT_HEARTBEAT);
                startHeartbeat(message.getLong(TransferStructureFactory.COMMAND_HEARTBEAT, heartbeat));
                LOGGER.debug("Heartbeats started");
                promise.complete(body);
            } else {
                promise.fail(h.cause());
            }
        });

        return promise.future();
    }

    private void logErrorDetails(JsonObject header, JsonObject body, String baseMessage) {
        StringBuilder errorLogBuilder = new StringBuilder(baseMessage);
        String errorMessage = body.getString(ERROR_MESSAGE_TAG);
        errorLogBuilder.append(errorMessage);
        Integer errorCode = header.getInteger(ERROR_CODE_TAG);
        errorLogBuilder.append("; error code: ").append(errorCode);
        errorLogBuilder.append(", error body: ").append(body.toString());
        LOGGER.error(errorLogBuilder.toString());
    }

    private void startHeartbeat(long time) {
        if (heartbeatTimerId != null) {
            LOGGER.info("canceled heartbeat timer: " + cancelTimer(heartbeatTimerId));
        }
        heartbeatTimerId = vertx.setPeriodic(time, ignored
                -> connectionHandler.sendHeartBeat(SM_ADDRESS, SM_TIMEOUT, getModuleState(), (JsonObject event1) -> {
                    LOGGER.debug("got result for heartbeat " + event1);
                    if (!event1.isEmpty()) {
                        LOGGER.error("Error while sending heartbeat: {0}. Resetting connection.", event1);
                        resetConnection();
                    }
                })
        );
    }

    /**
     * Get the inner state of the module. The default implementation calculates
     * the state based on the state of it's circuit breaker. Might be
     * overwritten to use a different measurement.
     *
     * @return The current state of the module
     */
    protected UPModuleState getModuleState() {
        return UPStatusUtils.fromCircuitBreakerState(breaker.state());
    }

    protected void unregisterFromServer() {
        Handler<JsonObject> dummyHandler = (JsonObject message) -> {
        };
        Map<String, Object> args = new HashMap<>();
        args.put("id", id);
        connectionHandler.sendCommand(SM_ADDRESS, "unregister", args, SM_TIMEOUT, dummyHandler);
    }

    public void deployVerticle(String verticleName, JsonObject verticleConfig, int verticleInstances, Handler<AsyncResult<String>> handler) {
        LOGGER.info("deploying " + verticleInstances + " verticles [" + verticleName + "]");
        if (verticleInstances > 0) {
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setConfig(verticleConfig);
            deploymentOptions.setInstances(verticleInstances);
            vertx.deployVerticle(verticleName, deploymentOptions, handler);
        }
    }

    public void deployWorkerVerticle(String verticleName, JsonObject verticleConfig, int verticleInstances, Handler<AsyncResult<String>> handler) {
        LOGGER.info("deploying " + verticleInstances + " worker verticles [" + verticleName + "]");
        if (verticleInstances > 0) {
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setConfig(verticleConfig);
            deploymentOptions.setInstances(verticleInstances);
            deploymentOptions.setWorker(true);
            vertx.deployVerticle(verticleName, deploymentOptions, handler);
        }
    }

    public void undeployVerticle(String deploymentId, Handler<AsyncResult<Void>> handler) {
        LOGGER.info("undeploying verticle [" + deploymentId + "]");
        vertx.undeploy(deploymentId, handler);
    }

    public long setTimer(long time, Handler<Long> handler) {
        return vertx.setTimer(time, handler);
    }

    public long setPeriodic(long time, Handler<Long> handler) {
        return vertx.setPeriodic(time, handler);
    }

    public boolean cancelTimer(long id) {
        return vertx.cancelTimer(id);
    }

    public boolean isEmptyConfig() {
        LOGGER.debug("Checking for non-empty config");
        boolean isEmptyConfig = config().isEmpty();
        if (isEmptyConfig) {
            LOGGER.fatal("Config is empty and thus not valid");
        } else {
            LOGGER.debug("Config is non-empty and thus considered valid");
        }
        return isEmptyConfig;
    }

    private void registerNewResetTimerIfNeeded() {
        if (resetTimerId == null) {
            resetTimerId = vertx.setTimer(SM_RETRY_TIME, timerId
                    -> resetConnection()
            );
            LOGGER.debug("Registered new reset timer.");
        }
    }

    private boolean cancelResetTimerIfPresent() {
        boolean deleted = false;
        if (resetTimerId != null) {
            LOGGER.debug("Cancelling postponed reset timer left.");
            deleted = vertx.cancelTimer(resetTimerId);
            resetTimerId = null;
        }
        return deleted;
    }

}
