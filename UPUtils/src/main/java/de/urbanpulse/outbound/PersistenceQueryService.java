package de.urbanpulse.outbound;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * service interface for querying persisted events
 * <p>
 * use {@link PersistenceQueryServiceProxyFactory#createProxy(io.vertx.core.Vertx, java.lang.String)
 * PersistenceQueryServiceProxyFactory.createProxy(vertx, address)}
 * to create a client proxy instance
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ProxyGen
@VertxGen
public interface PersistenceQueryService {

    /**
     * Query a persistence module's event storage.This can be invoked locally or across the cluster.<p>
     * when execution has started, we periodically send to uniqueMessageHandle a JsonObject like this:
     * <pre>
     * {
     *    "batch": [ ... events, may be an empty array ... ]
     * }
     * </pre>
     * <p>
     * the end of a successful query is indicated by sending to uniqueMessageHandle a JsonObject like this:
     * <pre>
     * {
     *    "batch": [ ... remaining events, may be an empty array ... ],
     *    "isLast": true
     * }
     * </pre>
     * <p>
     * any exception will abort the query and we try sending to uniqueMessageHandle a JsonObject like this:
     * <pre>
     * {
     *    "abortingException": "RuntimeException: blabla ...."
     * }
     * </pre>
     *
     * @param queryConfig contains the query parameters
     * @param uniqueRequestHandle unique address on the vert.x eventbus for this query which will receive the batches
     * @param resultHandler called with a successful {@link AsyncResult} if query execution was started successfully,
     * otherwise called with a failed one
     */
    void query(QueryConfig queryConfig, String uniqueRequestHandle,
            Handler<AsyncResult<Void>> resultHandler);

}
