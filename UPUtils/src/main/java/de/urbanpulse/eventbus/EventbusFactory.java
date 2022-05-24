package de.urbanpulse.eventbus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.InvocationTargetException;

import static de.urbanpulse.eventbus.helpers.EventBusImplementationDefaultsHolder.DEFAULT_EVENTBUS_FACTORY;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface EventbusFactory {

    static EventbusFactory createFactory(Vertx vertx, JsonObject jsonObject) {
        String clazz = jsonObject.getString("class", DEFAULT_EVENTBUS_FACTORY.toString());
        try {
            return (EventbusFactory) Class.forName(clazz)
                    .getConstructor(Vertx.class, JsonObject.class)
                    .newInstance(vertx, jsonObject);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid config!", e);
        }
    }

    void createMessageConsumer(JsonObject config, Handler<AsyncResult<MessageConsumer>> handler);

    void createMessageProducer(Handler<AsyncResult<MessageProducer>> handler);

}
