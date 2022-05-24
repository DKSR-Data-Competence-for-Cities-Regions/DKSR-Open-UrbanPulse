package de.urbanpulse.persistence.v3.storage;

import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_ADDRESS_PROPERTY;
import static de.urbanpulse.persistence.v3.storage.StorageServiceProviderVerticle.SERVICE_CLASS_PROPERTY;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class StorageServiceProviderVerticleTest {

    private Vertx vertx;

    @Before
    public void init() {
        vertx = Vertx.vertx();
        MockitoAnnotations.initMocks(this);

    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    /**
     * Test of stop method, of class StorageServiceProviderVerticle.
     *
     * @param context
     */
    @Test
    public void testDeployment(TestContext context) {
        JsonObject config = new JsonObject();
        config.put(SERVICE_ADDRESS_PROPERTY, "TestAddress");
        config.put(SERVICE_CLASS_PROPERTY, TestStorageServiceImpl.class.getName());
        config.put("responseAddress", "test");
        Async a = context.async(2);
        AtomicBoolean startReceived = new AtomicBoolean(false);
        MessageConsumer<String> c = vertx.eventBus().<String>consumer("test", m -> {
            System.out.println(m.body());
            if ("start".equals(m.body())) {
                startReceived.set(true);
                a.countDown();
            }
            if ("stop".equals(m.body()) && startReceived.get()) {
                a.countDown();
            }
        });
        vertx.deployVerticle(StorageServiceProviderVerticle.class.getName(), new DeploymentOptions().setConfig(config), result -> {
            String depId = result.result();
            vertx.undeploy(depId, context.asyncAssertSuccess());

        });


    }

}
