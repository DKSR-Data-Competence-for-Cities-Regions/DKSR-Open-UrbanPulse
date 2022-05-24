
package de.urbanpulse.upservice;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.List;

/**
 * Implementations of this class are responsible for giving us certain meta-information from UP.
 * So far, the only implementation is {@link UPServiceVerticle} which does so by querying the
 * UP database directly.
 * Also, only resolving event types to SIDs is implemented yet.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@VertxGen
@ProxyGen
public interface UPService {

    void getSIDsForEventTypeId(String eventTypeId, Handler<AsyncResult<List<String>>> resultHandler);
    void getSIDsForEventTypeName(String eventTypeName, Handler<AsyncResult<List<String>>> resultHandler);
    void getEventTypeNameForEventTypeId(String eventTypeId, Handler<AsyncResult<String>> resultHandler);
    void eventTypeIdExists(String eventTypeName, Handler<AsyncResult<Boolean>> resultHandler);
    void eventTypeNameExists(String eventTypeId, Handler<AsyncResult<Boolean>> resultHandler);
}
