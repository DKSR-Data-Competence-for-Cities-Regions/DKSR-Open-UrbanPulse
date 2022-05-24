package de.urbanpulse.dist.inbound.http;

import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MainVerticle extends AbstractMainVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    private String deploymentId = null;
    private HttpInboundCommandHandler httpInboundCommandHandler;

    @Override
    protected CommandHandler createCommandHandler() {
        httpInboundCommandHandler = new HttpInboundCommandHandler(this);
        return httpInboundCommandHandler;
    }

    @Override
    public void resetModule(Handler<Void> callback) {
        callback.handle(null);
    }

    @Override
    public void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
        if (setup.isEmpty()) {
            LOGGER.error("setupModule() got called with an empty object. Did you forget to configure the up_inbound_setup table in your database?");
            setupResultHandler.handle(Future.failedFuture("Empty config!"));
        } else {
            try {
                httpInboundCommandHandler.setup(setup);
                Map<String, Object> configMap = new HashMap<>(config().getJsonObject("clientConfig").getMap());
                configMap.putAll(setup.getMap());
                JsonObject moduleConfig = new JsonObject(configMap);
                moduleConfig.put("eventBusImplementation",config().getJsonObject("eventBusImplementation"));
                deployReceiver(moduleConfig);
            } catch (Exception e) {
                setupResultHandler.handle(Future.failedFuture(e.getCause()));
            }
            setupResultHandler.handle(Future.succeededFuture());
        }
    }

    @Override
    protected Map<String, Object> createRegisterModuleConfig() {
        Map<String, Object> args = new HashMap<>();
        args.put("moduleType", "InboundInterface");
        return args;
    }

    @Override
    public void start() {
        LOGGER.info("Starting HttpInboundModule MainVerticle");

        final long startDelay = config().getLong("startDelay", 1000L);
        LOGGER.info("waiting " + startDelay + " msec before deployment of verticles...");
        setTimer(startDelay, (Long) -> {
            super.start();
        });
    }

    private void deployReceiver(JsonObject config) {
        if (deploymentId != null) {
            String oldDeploymentId = deploymentId;
            deploymentId = null;
            undeployVerticle(oldDeploymentId, (AsyncResult<Void> event) -> {
                if (event.succeeded()) {
                    LOGGER.info("undeployed receiver with id " + oldDeploymentId);
                } else {
                    LOGGER.info("can't undeploy receiver with id " + oldDeploymentId, event.cause());
                }
            });
        }

        Handler<AsyncResult<String>> deployHandler = (AsyncResult<String> event) -> {
            if (event.succeeded()) {
                deploymentId = event.result();
                LOGGER.info("deployed receiver with id " + deploymentId);
            } else {
                LOGGER.info("can't deploy receiver", event.cause());
            }
        };
        //The server config wins if you change this in the config.json as well!
        int receiverCount = config.getInteger("receiverCount");
        LOGGER.info("deploying " + receiverCount + " receiver verticles");
        deployWorkerVerticle(HttpReceiverVerticle.VERTICLE_NAME, config, receiverCount, deployHandler);
    }
}
