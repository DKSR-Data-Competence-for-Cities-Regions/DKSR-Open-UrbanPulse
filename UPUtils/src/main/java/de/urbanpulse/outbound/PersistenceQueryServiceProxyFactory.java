package de.urbanpulse.outbound;

import io.vertx.core.Vertx;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface PersistenceQueryServiceProxyFactory {

    static PersistenceQueryServiceVertxEBProxy createProxy(Vertx vertx, String address) {
        return new PersistenceQueryServiceVertxEBProxy(vertx, address);
    }
}
