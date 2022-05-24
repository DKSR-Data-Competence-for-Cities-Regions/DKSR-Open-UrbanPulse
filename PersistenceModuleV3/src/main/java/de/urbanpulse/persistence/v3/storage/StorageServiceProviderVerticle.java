package de.urbanpulse.persistence.v3.storage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class StorageServiceProviderVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceProviderVerticle.class);

    public static final String SERVICE_CLASS_PROPERTY = "serviceClass";
    public static final String SERVICE_ADDRESS_PROPERTY = "serviceAddress";

    private ServiceBinder serviceBinder;
    private MessageConsumer<JsonObject> serviceProxyConsumer;
    private AbstractStorage storageServiceImpl;

    @Override
    public void start(Promise<Void> startPromise) {
        String serviceAddress = config().getString(SERVICE_ADDRESS_PROPERTY);
        String implementation = config().getString(SERVICE_CLASS_PROPERTY);

        serviceBinder = new ServiceBinder(vertx);

        Promise<Void> startService = Promise.promise();

        getStorage(implementation)
                .onSuccess(storage -> {
                    storageServiceImpl = storage;
                    storageServiceImpl.start(startService);
                })
                .onFailure(startService::fail);

        startService.future()
                .onSuccess(result -> {
                    serviceProxyConsumer = serviceBinder.setAddress(serviceAddress)
                            .register(StorageService.class, storageServiceImpl);
                    LOGGER.info(implementation + " listening on [" + serviceAddress + "] for queries");
                    startPromise.complete();
                }).onFailure(startPromise::fail);

    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        LOGGER.info("stopping storage.");
        if (serviceProxyConsumer != null) {
            serviceBinder.unregister(serviceProxyConsumer);
        }
        storageServiceImpl.stop(stopPromise);
    }

    protected Future<AbstractStorage> getStorage(String className) {
        Promise<AbstractStorage> result = Promise.promise();
        try {
            Class<? extends AbstractStorage> clazz = Class.forName(className).asSubclass(AbstractStorage.class);
            Constructor<? extends AbstractStorage> constructor = clazz.getConstructor(Vertx.class, JsonObject.class);
            AbstractStorage instance = constructor.newInstance(vertx, config());
            result.complete(instance);
        } catch (ClassCastException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOGGER.error("failed to instantiate storage implementation for class [" + className + "]");
            result.fail(ex);
        }
        return result.future();
    }

}
