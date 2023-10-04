package de.urbanpulse.persistence.v3;

import java.util.*;

import de.urbanpulse.persistence.v3.inbound.InboundVerticle;
import de.urbanpulse.persistence.v3.outbound.OutboundVerticle;
import de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle;
import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_ADDRESS_PROPERTY;
import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_CLASS_PROPERTY;
import static de.urbanpulse.persistence.v3.storage.cache.FirstLevelStorageConst.FIRST_LEVEL_STORAGE_SERVICE_ADDRESS;
import de.urbanpulse.persistence.v3.storage.cache.NullFirstLevelStorage;
import de.urbanpulse.transfer.CommandHandler;
import de.urbanpulse.transfer.vertx.AbstractMainVerticle;
import de.urbanpulse.transfer.vertx.DeploymentSpec;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * responsible for (un-)deployment of downstream verticles with appropriate
 * config
 */
public class MainVerticle extends AbstractMainVerticle {

    private static final String PARAM_STORAGE_CONFIG = "storageConfig";

    private final static Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private PersistenceCommandHandler persistenceCommandHandler;

    private final Deque<String> finishedDeployments = new LinkedList<>();
    private final Deque<DeploymentSpec> pendingDeployments = new LinkedList<>();

    @Override
    public void start() {
        LOGGER.info("Starting PersistenceV3 MainVerticle");

        String timezone = System.getProperty("user.timezone");
        if (!"UTC".equalsIgnoreCase(timezone)) {
            LOGGER.warn(" +++ timezone is NOT UTC! this can cause problems with temporal queries (especially around DST switch)! "
                    + "consider running with -Duser.timezone=UTC in JVM options or switch the OS to UTC +++ ");
        }

        // The command handler MUST be created before calling super.start()!
        persistenceCommandHandler = new PersistenceCommandHandler(this);
        super.start();
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        LOGGER.info("stop MainVerticle");
        stop();
        undeployAll(r -> {
            stopPromise.complete();
        });
    }

    @Override
    protected CommandHandler createCommandHandler() {
        return persistenceCommandHandler;
    }

    /**
     * called by command handler
     *
     * @param callback the callback Handler
     */
    @Override
    public void resetModule(Handler<Void> callback) {
        LOGGER.info("MainVerticle resetModule called");
        callback.handle(null);
    }

    @Override
    protected void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
        LOGGER.info("MainVerticle setupModule called");

        if (setup.isEmpty()) {
            setupResultHandler.handle(Future.failedFuture("setup configuration is empty"));
            return;
        }

        JsonObject obfuscatedConfig = LogHelper.configWithBlankedCredentials(setup);
        LOGGER.info("PersistenceV3 received setup[ " + obfuscatedConfig.encode() + " ]");

        LOGGER.info("cleaning existing deployments...");
        undeployAll((Boolean undeploySuccessful) -> {
            if (undeploySuccessful) {
                LOGGER.info("redeploying...");
                deployFromConfig(setup);
                setupResultHandler.handle(Future.succeededFuture());
            } else {
                setupResultHandler.handle(Future.failedFuture("failed to clear existing deployments"));
            }
        });
    }

    private void deployFromConfig(JsonObject config) {

        DeploymentSpec secondLevelStorageSpec = createSecondLevelStorageSpec(config);
        pendingDeployments.add(secondLevelStorageSpec);

        DeploymentSpec firstLevelStorageSpec = createFirstLevelStorageSpec(config);
        pendingDeployments.add(firstLevelStorageSpec);

        DeploymentSpec outboundSpec = createOutboundSpec(config);
        pendingDeployments.add(outboundSpec);

        DeploymentSpec inboundSpec = createInboundSpec(config);
        pendingDeployments.add(inboundSpec);

        deployAll((AsyncResult deploymentResult) -> {
            if (deploymentResult.succeeded()) {
                LOGGER.info("all succesfully deployed");
            } else {
                LOGGER.error("failed to deploy", deploymentResult.cause());
            }
        });
    }

    private DeploymentSpec createInboundSpec(JsonObject config) {
        JsonObject fileConfig = config();
        JsonObject inboundConfig = new JsonObject();
        JsonObject storageConfig = config.getJsonObject(PARAM_STORAGE_CONFIG, new JsonObject());
        inboundConfig.put(PARAM_STORAGE_CONFIG, storageConfig);
        inboundConfig.put("eventBusImplementation", fileConfig.getJsonObject("eventBusImplementation"));

        int instances = fileConfig.getInteger("inboundInstances", 2);
        String inputAddress = fileConfig.getString("inputAddress", "thePersistence");
        inboundConfig.put("inputAddress", inputAddress);
        String pullAddress = fileConfig.getString("pullAddress", "thePersistencePull");
        inboundConfig.put("pullAddress", pullAddress);

        DeploymentSpec inboundSpec = new DeploymentSpec(InboundVerticle.class.getName(), inboundConfig, instances, false);
        return inboundSpec;
    }

    private DeploymentSpec createFirstLevelStorageSpec(JsonObject config) {
        JsonObject storageServiceProviderConfig = config.getJsonObject(PARAM_STORAGE_CONFIG, new JsonObject());
        JsonObject firstLevelConfig = storageServiceProviderConfig.getJsonObject("firstLevelConfig", new JsonObject());
        String implementation = firstLevelConfig.getString("implementation", NullFirstLevelStorage.class.getName());

        storageServiceProviderConfig.put(SERVICE_CLASS_PROPERTY, implementation);
        storageServiceProviderConfig.put(SERVICE_ADDRESS_PROPERTY, FIRST_LEVEL_STORAGE_SERVICE_ADDRESS);

        return new DeploymentSpec(StorageServiceProviderVerticle.class.getName(), storageServiceProviderConfig, 1, true);
    }

    private DeploymentSpec createSecondLevelStorageSpec(JsonObject config) {
        int instances = config().getInteger("secondLevelStorageInstances", 1);
        LOGGER.debug("Using {0} secondLevelStorageInstances", instances);
        String pullAddress = config().getString("pullAddress", "thePersistencePull");
        String querySecondLevelAddress = config().getString("querySecondLevelAddress", "thePersistenceSecondLevelQuery");
        JsonObject secondLevelConfig = config.getJsonObject(PARAM_STORAGE_CONFIG).getJsonObject("secondLevelConfig");

        JsonObject storageServiceProviderConfig = new JsonObject();
        storageServiceProviderConfig.put("pullAddress", pullAddress);
        storageServiceProviderConfig.put(SERVICE_ADDRESS_PROPERTY, querySecondLevelAddress);
        storageServiceProviderConfig.put("secondLevelConfig", secondLevelConfig);

        //Support for legacy configs
        String implementation = new ServiceTranslator().convert(secondLevelConfig.getString("implementation"));

        if (implementation == null) {
            throw new IllegalStateException("No Second Level Storage configured!");
        }
        storageServiceProviderConfig.put(SERVICE_CLASS_PROPERTY, implementation);
        return new DeploymentSpec(StorageServiceProviderVerticle.class.getName(), storageServiceProviderConfig, instances, true);
    }

    private DeploymentSpec createOutboundSpec(JsonObject config) {
        JsonObject fileConfig = config();
        JsonObject outboundConfig = new JsonObject();

        JsonObject storageConfig = config.getJsonObject(PARAM_STORAGE_CONFIG, new JsonObject());
        outboundConfig.put(PARAM_STORAGE_CONFIG, storageConfig);

        JsonObject userAuth = config.getJsonObject("userAuth", new JsonObject());
        outboundConfig.put("userAuth", userAuth);

        outboundConfig.put("queryAddress", fileConfig.getString("queryAddress", "thePersistenceQuery"));

        String querySecondLevelAddress = fileConfig.getString("querySecondLevelAddress", "thePersistenceSecondLevelQuery");
        outboundConfig.put("querySecondLevelAddress", querySecondLevelAddress);

        int instances = fileConfig.getInteger("outboundInstances", 10);
        DeploymentSpec outboundSpec = new DeploymentSpec(OutboundVerticle.class.getName(), outboundConfig, instances, false);
        return outboundSpec;
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
        args.put("moduleType", "PersistenceV3");
        return args;
    }
}
