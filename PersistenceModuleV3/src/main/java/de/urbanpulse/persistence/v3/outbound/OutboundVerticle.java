package de.urbanpulse.persistence.v3.outbound;

import de.urbanpulse.outbound.PersistenceQueryService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * outbound verticle, can be queried via the vert.x eventbus using a service
 * proxy
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundVerticle extends AbstractVerticle {

    private final static Logger LOGGER = LoggerFactory.getLogger(OutboundVerticle.class);

    private String queryAddress;
    private MessageConsumer<JsonObject> serviceProxyConsumer;

    private ServiceBinder serviceBinder;

    @Override
    public void start(Promise<Void> startPromise) {
        queryAddress = config().getString("queryAddress");

        serviceBinder = new ServiceBinder(vertx);
        OutboundPersistenceQueryService queryServiceImpl = new OutboundPersistenceQueryService(vertx, config());
        serviceProxyConsumer = serviceBinder.setAddress(queryAddress).register(PersistenceQueryService.class, queryServiceImpl);

        LOGGER.info("listening on [" + queryAddress + "] for cluster queries");
        startPromise.complete();

    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        LOGGER.info("stopping");
        serviceBinder.unregister(serviceProxyConsumer);
        stopPromise.complete();
    }

}
