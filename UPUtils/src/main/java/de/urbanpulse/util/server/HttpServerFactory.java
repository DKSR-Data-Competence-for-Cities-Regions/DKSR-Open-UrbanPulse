package de.urbanpulse.util.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.SelfSignedCertificate;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpServerFactory {

    private static final Logger LOG = Logger.getLogger(HttpServerFactory.class.getName());

    private HttpServerFactory() {
    }

    /**
     * @param vertx reference to Vert.x instance
     * @param config contains encrypt flag, keystore, keystorePassword and
     * (optionally) cipherSuites (the latter three are used only if encrypt is
     * true)
     * @param encrypt if the response should be encrypted
     * @return HttpServer
     */
    public static HttpServer createHttpServer(Vertx vertx, JsonObject config, boolean encrypt) {
        HttpServerOptions options = new HttpServerOptions();
        boolean keepAlive = config.getBoolean("keepAlive", false);
        String host = config.getString("host");

        options.setTcpKeepAlive(keepAlive);
        options.setSsl(encrypt);
        options.setHost(host);
        options.setSni(true);
        LOG.info("SNI enabled.");
        if (encrypt) {
            String keystore = config.getString("keystore", "");
            if (keystore.isEmpty()) { //only used for debug purposes. when one wants to test a secure connection but no keystore can be assumed
                LOG.info("Using self signed certificate!");
                SelfSignedCertificate certificate = SelfSignedCertificate.create("localhost");
                options.setKeyCertOptions(certificate.keyCertOptions());
            } else {
                String keystorePassword = config.getString("keystorePassword");
                JksOptions jksOptions = new JksOptions();
                jksOptions.setPath(keystore);
                jksOptions.setPassword(keystorePassword);
                options.setKeyStoreOptions(jksOptions);
            }

            JsonArray cipherSuites = config.getJsonArray("cipherSuites", new JsonArray());
            for (Object suite : cipherSuites) {
                options.addEnabledCipherSuite((String) suite);
            }
        }

        return vertx.createHttpServer(options);
    }

    public static HttpServer createHttpServer(Vertx vertx, JsonObject config) {
        boolean encrypt = config.getBoolean("encrypt", true);
        return createHttpServer(vertx, config, encrypt);
    }
}
