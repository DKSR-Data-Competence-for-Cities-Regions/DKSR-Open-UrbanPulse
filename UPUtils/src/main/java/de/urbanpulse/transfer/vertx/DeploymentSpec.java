package de.urbanpulse.transfer.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * defines a verticle deployment
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class DeploymentSpec {

    private final String verticleName;
    private final JsonObject verticleConfig;
    private final int verticleInstances;
    private final boolean isWorker;

    /**
     * create a new non-worker deployment spec
     * @param verticleName the name of the verticle to be deployed
     * @param verticleConfig the configuration for the verticle
     * @param verticleInstances the number of instances for this verticle
     */
    public DeploymentSpec(String verticleName, JsonObject verticleConfig, int verticleInstances) {
        this(verticleName, verticleConfig, verticleInstances, false);
    }

    /**
     * create a new deployment spec
     * @param verticleName the name of the verticle to be deployed
     * @param verticleConfig the configuration for the verticle
     * @param verticleInstances the number of instances for this verticle
     * @param isWorker if this verticle is a worker verticle
     */
    public DeploymentSpec(String verticleName, JsonObject verticleConfig, int verticleInstances, boolean isWorker) {
        this.verticleName = verticleName;
        this.verticleConfig = verticleConfig;
        this.verticleInstances = verticleInstances;
        this.isWorker = isWorker;
    }

    /**
     * let the {@link AbstractMainVerticle} deploy this spec
     *
     * @param mainVerticle the main verticle (the entry point)
     * @param handler reports success/failure and in case of success the deploymentId
     */
    public void deploy(AbstractMainVerticle mainVerticle, Handler<AsyncResult<String>> handler) {
        if (isWorker) {
            mainVerticle.deployWorkerVerticle(verticleName, verticleConfig, verticleInstances, handler);
        } else {
            mainVerticle.deployVerticle(verticleName, verticleConfig, verticleInstances, handler);
        }
    }
}
