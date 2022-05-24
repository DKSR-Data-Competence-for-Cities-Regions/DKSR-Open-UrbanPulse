package de.urbanpulse.dist.outbound;

import de.urbanpulse.dist.outbound.client.HttpVerticle;
import de.urbanpulse.dist.outbound.mailer.service.OutboundMailerControllerVerticle;
import de.urbanpulse.dist.outbound.server.ws.WsPublisherVerticle;
import de.urbanpulse.dist.outbound.server.ws.WsServerVerticle;
import de.urbanpulse.dist.outbound.server.auth.SecurityManagerInitializer;
import de.urbanpulse.dist.outbound.server.historicaldata.HistoricalDataRestVerticle;
import de.urbanpulse.upservice.UPServiceVerticle;
import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.UndoCommand;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.shiro.SecurityUtils;

/**
 * Deploy only one instance per OutboundModule!
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MainVerticle extends AbstractMainVerticle {

    /**
     * prefix for the event bus address of a statement for global publishing
     * across the cluster
     * <p>
     * (used by {@link WsPublisherVerticle} to send and by
     * {@link WsServerVerticle} to receive )
     */
    public static final String GLOBAL_STATEMENT_PREFIX = "global-statement:";

    /**
     * prefix for the event bus address of a statement for local publishing
     * within one OutboundModule instance
     * <p>
     * (used by {@link MainVerticle} to send and by {@link WsPublisherVerticle}
     * and {@link HttpVerticle} to receive)
     */
    public static final String LOCAL_STATEMENT_PREFIX = "local-statement:";

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private OutboundCommandHandler outboundCommandHandler;

    @Override
    protected CommandHandler createCommandHandler() {
        outboundCommandHandler = new OutboundCommandHandler(this);
        return outboundCommandHandler;
    }

    public void setOutboundCommandHandler(OutboundCommandHandler commandHandler) {
        outboundCommandHandler = commandHandler;
    }

    @Override
    public void resetModule(Handler<Void> callback) {
        outboundCommandHandler.reset((JsonObject, UndoCommand) -> {
            callback.handle(null);
        });
    }

    @Override
    protected Map<String, Object> createRegisterModuleConfig() {
        Map<String, Object> args = new HashMap<>();
        args.put("moduleType", "OutboundInterface");
        return args;
    }

    @Override
    public void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
        outboundCommandHandler.setup(setup, (JsonObject json, UndoCommand undoCommand) -> {
            boolean wasSuccessful = json == null;

            if (wasSuccessful) {
                setupResultHandler.handle(Future.succeededFuture());
            } else {
                setupResultHandler.handle(Future.failedFuture("Setup failed."));
            }
        });
    }

    @Override
    public void start(Promise<Void> startPromise) {

        // This check is copied from start(Future) method from AbstractVerticle
        // because Outbound MainVerticle overwrites the
        // parent method with further functionality
        if (isEmptyConfig()) {
            startPromise.fail("Config is empty (or corrupted) and thus not valid");
            return;
        }

        Optional<JsonObject> securityConfig = Optional.ofNullable(config().getJsonObject("MainVerticle", new JsonObject()).getJsonObject("security"));

        if (securityConfig.isPresent()) {
            try {
                SecurityUtils.getSecurityManager();
                LOGGER.warn("External Security Manager in use!");
            } catch (org.apache.shiro.UnavailableSecurityManagerException usme) {
                LOGGER.info("Initializing SecurityManager.");
                SecurityManagerInitializer.initSecurityManager(securityConfig.get());
            } catch (Exception e) {
                LOGGER.error("Security manager initialization failed!", e);
                startPromise.fail(e);
                return;
            }
        } else {
            startPromise.fail("Security manager config missing!");
            return;
        }

        CompositeFuture.all(deployOutboundMailer(), deployWsServerVerticle(), deployUPServiceVerticle()).onComplete(handler -> {
            if (handler.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(handler.cause());
            }
        });
    }

    private Future<Void> deployWsServerVerticle() {
        LOGGER.info("Deploying Websocket Server...");
        Promise<Void> result = Promise.promise();
        JsonObject wsServerConfig = config().getJsonObject("wsServerConfig");

        applySharedServerConfigFieldsTo(wsServerConfig);

        deployVerticle(WsServerVerticle.class.getName(), wsServerConfig, 1, (AsyncResult<String> asyncResult) -> {
            handleWsServerDeploymentResult(asyncResult, wsServerConfig, result);
        });
        return result.future();
    }

    private Future<Void> deployOutboundMailer() {
        JsonObject outboundMailerConfig = config().getJsonObject("outboundMailerConfig", new JsonObject());
        if (outboundMailerConfig.isEmpty()) {
            LOGGER.info("Skipping Outbound Mailer deployment due to empty config.");
            return Future.succeededFuture();
        } else {
            Promise<Void> result = Promise.promise();
            LOGGER.info("Deploying Outbound Mailer...");
            deployVerticle(OutboundMailerControllerVerticle.class.getName(), outboundMailerConfig, 1, (AsyncResult<String> asyncResult) -> {
                if (asyncResult.succeeded()) {
                    LOGGER.info("Outbound Mailer deployed.");
                    result.complete();
                } else {
                    result.fail("OutboundMailerController deployment failed!");
                }
            });
            return result.future();
        }
    }

    private Future<Void> deployUPServiceVerticle() {
        JsonObject upServiceVerticleConfig = config().getJsonObject("upServiceConfig", new JsonObject());
        if (upServiceVerticleConfig.isEmpty()) {
            return Future.failedFuture("upServiceConfig needs to be provided");
        } else {
            Promise<Void> result = Promise.promise();
            LOGGER.info("Deploying UPService...");
            deployVerticle(UPServiceVerticle.class.getName(), upServiceVerticleConfig, 1, asyncResult -> {
                if (asyncResult.succeeded()) {
                    LOGGER.info("UPServiceVerticle deployed");
                    result.complete();
                } else {
                    result.fail(asyncResult.cause());
                }
            });
            return result.future();
        }
    }

    private void handleWsServerDeploymentResult(AsyncResult<String> asyncResult, JsonObject wsServerConfig, Promise<Void> startPromise)
            throws RuntimeException {
        if (asyncResult.succeeded()) {
            LOGGER.info("deploying ws publisher...");
            deployVerticle(WsPublisherVerticle.class.getName(), wsServerConfig, 1, (AsyncResult<String> asyncResult2) -> {
                handleWsPublisherDeploymentResult(asyncResult2, startPromise);
            });
        } else {
            startPromise.fail("WS server deployment failed!");
        }
    }

    private void handleWsPublisherDeploymentResult(AsyncResult<String> asyncResult, Promise<Void> startPromise)
            throws RuntimeException {
        if (asyncResult.succeeded()) {
            LOGGER.info("deploying incoming event receiver...");
            deployVerticle(IncomingEventReceiverVerticle.class.getName(), config(), 1, (AsyncResult<String> asyncResult3) -> {
                handleIncomingEventReceiverDeploymentResult(asyncResult3, startPromise);
            });
        } else {
            startPromise.fail("WS publisher deployment failed!");
        }
    }

    private void handleIncomingEventReceiverDeploymentResult(AsyncResult<String> asyncResult, Promise<Void> startPromise) {
        if (asyncResult.succeeded()) {
            LOGGER.info("deploying historical data rest service...");
            JsonObject restConfig = config().getJsonObject("historicalDataRestConfig");

            Integer instances = null;
            if (restConfig.getInteger("httpVerticleInstances") != null) {
                instances = restConfig.getInteger("httpVerticleInstances");
            }

            // Backward compatibility.
            if (instances == null && config().getInteger("httpVerticleInstances") != null) {
                LOGGER.warn("Deprecated config found: httpVerticleInstances should be configured within the historicalDataRestConfig");
                instances = config().getInteger("httpVerticleInstances");
            }

            if (instances == null) {
                instances = 1;
            }

            applySharedServerConfigFieldsTo(restConfig);

            // rest config did originally not specify any host, hence put it here as default for backwards compatibility if absent
            if (!restConfig.containsKey("host")) {
                restConfig.put("host", "0.0.0.0");
            }

            deployVerticle(HistoricalDataRestVerticle.class.getName(), restConfig, instances, (AsyncResult<String> asyncResult3) -> {
                handleHistoricalDataRestDeploymentResult(asyncResult3, startPromise);
            });
        } else {
            startPromise.fail("incoming event receiver deployment failed!");
        }
    }

    /**
     * if config() contains a section "sharedServerConfig", copy any of these
     * five fields present there but absent from targetConfig to targetConfig:
     * <ul>
     * <li>host (string)</li>
     * <li>encrypt (boolean)</li>
     * <li>keystore (string)</li>
     * <li>keystorePassword (string)</li>
     * <li>cipherSuites (array of strings)</li>
     * </ul>
     *
     * @param targetConfig the JsonObject representing the targeted config
     */
    private void applySharedServerConfigFieldsTo(JsonObject targetConfig) {
        JsonObject sharedServerConfig = config().getJsonObject("sharedServerConfig", new JsonObject()).copy();

        if (!targetConfig.containsKey("host") && sharedServerConfig.containsKey("host")) {
            targetConfig.put("host", sharedServerConfig.getString("host"));
        }

        if (!targetConfig.containsKey("encrypt") && sharedServerConfig.containsKey("encrypt")) {
            targetConfig.put("encrypt", sharedServerConfig.getBoolean("encrypt"));
        }

        if (!targetConfig.containsKey("cipherSuites") && sharedServerConfig.containsKey("cipherSuites")) {
            targetConfig.put("cipherSuites", sharedServerConfig.getJsonArray("cipherSuites").copy());
        }

        if (!targetConfig.containsKey("keystorePassword") && sharedServerConfig.containsKey("keystorePassword")) {
            targetConfig.put("keystorePassword", sharedServerConfig.getString("keystorePassword"));
        }

        if (!targetConfig.containsKey("keystore") && sharedServerConfig.containsKey("keystore")) {
            targetConfig.put("keystore", sharedServerConfig.getString("keystore"));
        }
    }

    private void handleHistoricalDataRestDeploymentResult(AsyncResult<String> asyncResult, Promise<Void> startPromise) {
        if (asyncResult.succeeded()) {
            LOGGER.info("finishing main startup...");
            super.start();
            startPromise.complete();
        } else {
            startPromise.fail("historical data rest service deployment failed!");
        }
    }

    @Override
    public void deployWorkerVerticle(String verticleName, JsonObject verticleConfig, int verticleInstances,
            Handler<AsyncResult<String>> handler) {
        JsonObject combinedConfig = verticleConfig.copy().put("clientConfig", getModuleConfig());
        super.deployWorkerVerticle(verticleName, combinedConfig, verticleInstances, handler);
    }
}
