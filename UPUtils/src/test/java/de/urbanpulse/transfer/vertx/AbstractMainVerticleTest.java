package de.urbanpulse.transfer.vertx;

import de.urbanpulse.transfer.CommandHandler;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class AbstractMainVerticleTest {

    MainVerticleImpl mainVerticle;
    Vertx vertx;


    public void deployMainVerticle(Async async) {
        try {
            vertx = Vertx.vertx();
            mainVerticle = new MainVerticleImpl(getConfig());
            DeploymentOptions options = new DeploymentOptions().setConfig(getConfig());

            // to prevent calling registerAtServer. Otherwise need to go through ConnectionHandler & TransportLayer
            Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
            isRegisteringField.setAccessible(true);
            ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

            vertx.deployVerticle(mainVerticle, options, hndlr -> {
                try {
                    assertTrue(hndlr.succeeded());
                    ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
                    async.complete();
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    assertFalse("Exception thrown whil reflection.", true);
                }
            });
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            assertFalse("Exception thrown whil reflection.", true);
        }
    }

    @Test
    public void test_start_valid_config(TestContext context) {
        Async async = context.async();
        deployMainVerticle(async);
        async.await(6000);
        vertx.close();
        vertx = null;
        mainVerticle = null;
    }

    /**
     * Without circuitBreakerOptions in the config, default values should be
     * used
     *
     * @throws NoSuchMethodException method could not be found
     * @throws IllegalAccessException the currently executing method does not
     * have access to the definition of the specified class, field, method or
     * constructor.
     * @throws IllegalArgumentException input is incorrect or missing.
     * @throws InvocationTargetException reflection error
     */
    @Test
    public void testGetCircuitBreakerOptions_willUseDefaultValuesIfNotPresentInConfig() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        JsonObject configWithoutCircuitBreaker = new JsonObject().put("heartbeat", 1000);

        MainVerticleImpl plainMainVerticle = new MainVerticleImpl(configWithoutCircuitBreaker);

        CircuitBreakerOptions options = (CircuitBreakerOptions) plainMainVerticle.getCircuitBreakerOptions();
        assertEquals(5, options.getMaxFailures());
        assertEquals(2000, options.getTimeout());
        assertEquals(10000, options.getResetTimeout());
    }

    /**
     * if circuitBreakerOptions are present in the config, these values should
     * be used
     *
     * @throws NoSuchMethodException method could not be found
     * @throws IllegalAccessException the currently executing method does not
     * have access to the definition of the specified class, field, method or
     * constructor.
     * @throws IllegalArgumentException input is incorrect or missing.
     * @throws InvocationTargetException reflection error
     */
    @Test
    public void testGetCircuitBreakerOptions_willUseConfigIfPresent() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final int failures = 123;
        final long timeout = 456l;
        final long resetTimeout = 789l;

        JsonObject configWithCircuitBreaker = new JsonObject()
                .put("circuitBreakerOptions", new JsonObject()
                        .put("maxFailures", failures)
                        .put("timeout", timeout)
                        .put("resetTimeout", resetTimeout)
                );

        MainVerticleImpl plainMainVerticle = new MainVerticleImpl(configWithCircuitBreaker);

        CircuitBreakerOptions options = (CircuitBreakerOptions) plainMainVerticle.getCircuitBreakerOptions();
        assertEquals(failures, options.getMaxFailures());
        assertEquals(timeout, options.getTimeout());
        assertEquals(resetTimeout, options.getResetTimeout());
    }

    /**
     * isEmptyConfig method should make setup fail when config is
     * empty/corrupted
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testIsEmptyConfig_emptyConfigShouldThrowException() throws Exception {
        System.out.println("testisEmptyConfig");

        JsonObject corruptedConfig = new JsonObject();
        MainVerticleImpl plainMainVerticle = new MainVerticleImpl(corruptedConfig);

        Promise startPromise = Promise.promise();
        plainMainVerticle.start(startPromise);

        Assert.assertTrue(startPromise.future().failed());
    }

    @Test
    public void test_cancelRestTimerIfPresent(TestContext context) {
        try {
            Async async = context.async();
            deployMainVerticle(async);
            async.await(6000);

            // to pretend restTimer is set
            Field resetTimerIdField = mainVerticle.getClass().getSuperclass().getDeclaredField("resetTimerId");
            resetTimerIdField.setAccessible(true);
            resetTimerIdField.set(mainVerticle, 1000L);

            mainVerticle.resetConnection();

            assertNull(resetTimerIdField.get(mainVerticle));


        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            assertFalse("Exception thrown whil reflection.", true);
        }

    }

    private JsonObject getConfig() {
        return new JsonObject().put("startDelay", 1000);
    }

    public class MainVerticleImpl extends AbstractMainVerticle {

        private boolean setupSucceeds = true;
        private final JsonObject verticleConfig;

        public MainVerticleImpl(JsonObject config) {
            this.verticleConfig = config;
        }

        public void setIfSetupModuleSucceeeds(boolean setupSucceds) {
            this.setupSucceeds = setupSucceds;
        }

        @Override
        protected CommandHandler createCommandHandler() {
            return new CommandHandler(this);
        }

        @Override
        protected void resetModule(Handler<Void> callback) {
            // do not need to run it here
        }

        @Override
        protected void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
            if (setupSucceeds) {
                setupResultHandler.handle(Future.succeededFuture());
            } else {
                setupResultHandler.handle(Future.failedFuture("failed"));
            }
        }

        @Override
        protected Map<String, Object> createRegisterModuleConfig() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long setPeriodic(long time, Handler<Long> handler) {
            return 23;
        }

        @Override
        public JsonObject config() {
            return verticleConfig;
        }

    }

}
