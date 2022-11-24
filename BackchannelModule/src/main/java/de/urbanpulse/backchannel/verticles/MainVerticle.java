package de.urbanpulse.backchannel.verticles;

import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import de.urbanpulse.transfer.vertx.DeploymentSpec;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MainVerticle extends AbstractMainVerticle {

    public static final String MODULE_TYPE = "Backchannel";

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    public static final String VERTICLE_NAME = MainVerticle.class.getName();


    private MessageConsumer<JsonObject> commandHandlerServiceProxyConsumer;
    private MessageConsumer<JsonObject> backchannelHandlerServiceProxyConsumer;
    private ServiceBinder backChannelHandlerServiceBinder;
    private ServiceBinder commandHandlerServiceBinder;

    private final Deque<String> finishedDeployments = new LinkedList<>();
    private final Deque<DeploymentSpec> pendingDeployments = new LinkedList<>();
    


    @Override
    public void resetModule(Handler<Void> callback) {
        LOGGER.info("MainVerticle resetModule called");
        callback.handle(null);
    }

    @Override
    protected void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
        LOGGER.info("MainVerticle setupModule called");

        if (setup.isEmpty()) {
            setupResultHandler.handle(Future.failedFuture("Empty setup received"));
            return;
        }

        Map<String, Object> configMap = config().getJsonObject("receiverConfig").getMap();
        configMap.putAll(setup.getMap());

        undeployAll((Boolean undeploySuccessful) -> {
            if (undeploySuccessful) {
                deployFromConfig(new JsonObject(configMap));
                setupResultHandler.handle(Future.succeededFuture());
            } else {
                LOGGER.error("failed to clear existing deployments");
                setupResultHandler.handle(Future.failedFuture("failed to clear existing deployments"));
            }
        });
    }

    private void deployFromConfig(JsonObject config) {
        DeploymentSpec httpServerVerticleSpec = createHttpServerVerticleSpec(config);
        pendingDeployments.add(httpServerVerticleSpec);

        deployAll((AsyncResult deploymentResult) -> {
            if (deploymentResult.succeeded()) {
                LOGGER.info("all successfully deployed");
            } else {
                LOGGER.error("failed to deploy", deploymentResult.cause());
            }
        });
    }

    private void deployAll(Handler<AsyncResult> resultHandler) {
        if (pendingDeployments.isEmpty()) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }

        DeploymentSpec spec = pendingDeployments.pop();
        spec.deploy(this, (AsyncResult<String> asyncResult) -> {
            if (asyncResult.succeeded()) {
                String deploymentId = asyncResult.result();
                finishedDeployments.add(deploymentId);
                LOGGER.info("deployment " + deploymentId + " successful");
                deployAll(resultHandler);
            } else {
                LOGGER.error("deployment failed, undeploying the rest...");
                undeployAll((Boolean ignoredBecauseItAlreadyFailed) -> {
                    resultHandler.handle(asyncResult);
                });
            }
        });
    }

    /**
     * undeploy all currently known deployments
     *
     * @param resultHandler reports success or failure
     */
    private void undeployAll(Handler<Boolean> resultHandler) {
        undeployAll(resultHandler, true);
    }

    private void undeployAll(Handler<Boolean> resultHandler, boolean allPreviousSuccessful) {
        if (finishedDeployments.isEmpty()) {
            resultHandler.handle(allPreviousSuccessful);
            return;
        }

        String deploymentId = finishedDeployments.pop();
        LOGGER.info("undeploying " + deploymentId);
        undeployVerticle(deploymentId, (AsyncResult<Void> asyncResult) -> {
            undeployAll(resultHandler, asyncResult.succeeded() && allPreviousSuccessful);
        });
    }

    @Override
    protected Map<String, Object> createRegisterModuleConfig() {
        Map<String, Object> args = new HashMap<>();
        args.put("moduleType", MODULE_TYPE);
        return args;
    }

    @Override
    protected CommandHandler createCommandHandler() {
        return null;
    }

    @Override
    public void start() {
        LOGGER.info("Starting Backchannel MainVerticle");
        
        backChannelHandlerServiceBinder = new ServiceBinder(vertx).setAddress(MODULE_TYPE + "SenderService");
        commandHandlerServiceBinder = new ServiceBinder(vertx).setAddress(MODULE_TYPE + "CommandHandler");

        super.start();
    }

    @Override
    public void stop() {
        if (commandHandlerServiceProxyConsumer != null) {
            commandHandlerServiceBinder.unregister(commandHandlerServiceProxyConsumer);
        }
        if (backchannelHandlerServiceProxyConsumer != null) {
            backChannelHandlerServiceBinder.unregister(backchannelHandlerServiceProxyConsumer);
        }
    }

    private DeploymentSpec createHttpServerVerticleSpec(JsonObject config) {
        int receiverCount = config.getInteger("receiverCount", 1);
        return new DeploymentSpec(HttpServerVerticle.class.getName(), config, receiverCount, true);
    }
}
