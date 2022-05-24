package de.urbanpulse.util.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class HttpServerFactoryTest {
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @Test
    public void test_unencrypted_server() {
        JsonObject config = new JsonObject().put("encrypt", false)
                .put("host", "localhost")
                .put("port", 0);

        HttpServer httpServer = HttpServerFactory.createHttpServer(vertx, config);
        assertNotNull(httpServer);
    }

    @Test
    public void test_encrypted_no_jks_file() {
        JsonObject config = new JsonObject().put("encrypt", true)
                .put("host", "localhost")
                .put("port", 0);

        HttpServer httpServer = HttpServerFactory.createHttpServer(vertx, config);
        assertNotNull(httpServer);
    }

    @Test
    public void test_encrypted_jks_file() {
        JsonObject config = new JsonObject().put("encrypt", true)
                .put("keystore", "localhost_keystore.jks")
                .put("keystorePassword", "cahngeit")
                .put("host", "localhost")
                .put("port", 0);

        HttpServer httpServer = HttpServerFactory.createHttpServer(vertx, config);
        assertNotNull(httpServer);
    }

}
