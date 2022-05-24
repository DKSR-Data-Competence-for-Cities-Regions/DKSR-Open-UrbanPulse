package de.urbanpulse.util;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AccessLogger {

    private static final String ACCESS_LOG_PREFIX = "accessLog.";

    private final Logger logger;

    public static AccessLogger newInstance(Class consumerClass) {
      return new AccessLogger(LoggerFactory.getLogger(ACCESS_LOG_PREFIX + consumerClass.getName()));
    }

    private AccessLogger(Logger logger) {
        this.logger = logger;
    }

    public void log(HttpServerRequest request) {
        String message = "Received HTTP request with " +
                method(request) + ",  " +
                path(request) + ",  " +
                remoteAddress(request) + ",  " +
                localAddress(request) + ",  " +
                host(request);
        logger.info(message);
    }

    private String host(HttpServerRequest request){
        return "host=" + request.host();
    }
    private String localAddress(HttpServerRequest request){
        return "localAddress=" + request.localAddress();
    }
    private String remoteAddress(HttpServerRequest request){
        return "remoteAddress=" + request.remoteAddress();
    }
    private String path(HttpServerRequest request){
        return "path=" + request.path();
    }
    private String method(HttpServerRequest request) {
        return "method=" + request.method();
    }
}
