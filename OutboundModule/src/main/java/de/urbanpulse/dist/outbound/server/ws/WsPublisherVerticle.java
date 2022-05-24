package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.dist.outbound.MainVerticle;
import de.urbanpulse.dist.util.StatementConsumerManagementVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;



/**
 * Deploy only one instance per OutboundModule!
 * <p>
 * To be able to support multiple deployments of OutboundModule behind a load-balancer,
 * events intended for the WS server (and ONLY those!) have to be sent to all OutboundModule instances in the cluster.
 * This verticle provides an intermediate layer that publishes events received locally on the event bus to all WS server
 * instances in the cluster.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsPublisherVerticle extends StatementConsumerManagementVerticle {

    public static final String SETUP_ADDRESS = WsPublisherVerticle.class.getName();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        statementPrefix = MainVerticle.LOCAL_STATEMENT_PREFIX;
        registerSetupConsumer(SETUP_ADDRESS);
        startPromise.complete();
    }


    @Override
    protected void handleEvent(String statementName, JsonObject event) {

        String globalStatementAddress = MainVerticle.GLOBAL_STATEMENT_PREFIX + statementName;
        vertx.eventBus().publish(globalStatementAddress, event);
    }




    @Override
    protected void reset() {
        statementToListenerMap.clear();
    }

}
