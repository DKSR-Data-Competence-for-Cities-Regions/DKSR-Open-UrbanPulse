package de.urbanpulse.dist.outbound.mailer.service;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
@ProxyGen
public interface OutboundMailerService {

    static OutboundMailerService create(Vertx vertx, JsonObject config) {
        return new OutboundMailerServiceImpl(vertx, config);
    }
    
    void sendMail(List<String> receiver, String subject, String message, boolean htmlText, Handler<AsyncResult<JsonObject>> handler);

    @ProxyClose
    void close();

}
