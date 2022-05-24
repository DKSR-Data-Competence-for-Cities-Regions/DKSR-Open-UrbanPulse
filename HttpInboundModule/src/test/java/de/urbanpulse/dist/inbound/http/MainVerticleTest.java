/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.dist.inbound.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import static org.junit.Assert.assertFalse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private final Logger LOGGER = LoggerFactory.getLogger(MainVerticleTest.class);

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test
    public void bug_2866_NullpointerExceptionTest() {
        class TestMainVerticle extends MainVerticle {
            private void deployReceiver(JsonObject config) {

            }
        }
        MainVerticle main = new TestMainVerticle();
        main.setupModule(new JsonObject(), h -> {
            assertFalse(h.succeeded());
        });
    }
}
