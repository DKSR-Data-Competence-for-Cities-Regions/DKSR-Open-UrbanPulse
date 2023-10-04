package de.urbanpulse.persistence.v3.storage;

import de.urbanpulse.outbound.QueryConfig;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
@ProxyGen
public interface StorageService {
    
   public void query(QueryConfig queryConfig, String uniqueRequestHandle, Handler<AsyncResult<Void>> resultHandler);
   public void persist(List<JsonObject> events);

}
