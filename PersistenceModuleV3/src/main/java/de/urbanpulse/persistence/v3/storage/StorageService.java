package de.urbanpulse.persistence.v3.storage;

import de.urbanpulse.outbound.QueryConfig;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ProxyGen
public interface StorageService {

   public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler);
   public void persist(List<JsonObject> events);

}
