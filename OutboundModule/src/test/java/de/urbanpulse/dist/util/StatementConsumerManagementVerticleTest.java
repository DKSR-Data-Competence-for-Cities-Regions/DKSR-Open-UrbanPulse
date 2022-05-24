package de.urbanpulse.dist.util;

import de.urbanpulse.dist.outbound.MainVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class StatementConsumerManagementVerticleTest {

    private static final String CONSUMER_ADDRESS = "CONSUMER_ADDRESS";
    private static final String STATEMENT_NAME = "TestStatement";
    private static final String TARGET = "aTarget";
    private static final String ANOTHER_TARGET = "AnotherTarget";
    private static final String ID = "The UUID of the UL";
    private static final JsonObject UL_CONFIG =
            new JsonObject().put("statementName", STATEMENT_NAME).put("target", TARGET).put("id", ID);
    private final UpdateListenerConfig UL_CONFIG_OBJECT = new UpdateListenerConfig(UL_CONFIG);
    private static final JsonObject UL_CONFIG_SAME_STATEMENT_OTHER_TARGET =
            new JsonObject().put("statementName", STATEMENT_NAME).put("target", ANOTHER_TARGET).put("id", ID);
    private final UpdateListenerConfig UL_CONFIG_SAME_STATEMENT_OBJECT =
            new UpdateListenerConfig(UL_CONFIG_SAME_STATEMENT_OTHER_TARGET);

    private static final String LOCAL_STATEMENT_ADDRESS = MainVerticle.LOCAL_STATEMENT_PREFIX + STATEMENT_NAME;

    private StatementConsumerManagementVerticleImpl uut;
    private String deploymentId;
    private Vertx vertx;


    private class StatementConsumerManagementVerticleImpl extends StatementConsumerManagementVerticle {

        @Override
        protected void handleEvent(String statementName, JsonObject event) {
        }
    }


    @Before
    public void setup(TestContext context) {

        Async async = context.async();
        StatementConsumerManagementVerticleImpl impl = new StatementConsumerManagementVerticleImpl();
        uut = spy(impl);

        vertx = Vertx.vertx();

        vertx.deployVerticle(uut, result -> {
            if (result.succeeded()) {
                deploymentId = result.result();
                uut.registerSetupConsumer(CONSUMER_ADDRESS);
            } else {
                context.fail(result.cause());
            }
            async.complete();
        });

    }

    @Test
    public void test_registerMessageRegistersConsumer(TestContext context) {

        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);

        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertTrue(reply.succeeded());

            context.assertEquals(1, uut.getStatementToConsumerMap().size());
            context.assertTrue(uut.getStatementToConsumerMap().containsKey(STATEMENT_NAME));
            context.assertEquals(LOCAL_STATEMENT_ADDRESS, uut.getStatementToConsumerMap().get(STATEMENT_NAME).address());

            context.assertEquals(1, uut.getStatementToListenerMap().get(STATEMENT_NAME).size());

            context.assertTrue(uut.getStatementToListenerMap().get(STATEMENT_NAME).contains(UL_CONFIG_OBJECT));

            async.complete();
        });
    }

    @Test
    public void test_registerMessageCallsRegisterMethod(TestContext context) {

        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);
        ArgumentCaptor<UpdateListenerConfig> configArgumentCaptor = ArgumentCaptor.forClass(UpdateListenerConfig.class);

        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertTrue(reply.succeeded());

            verify(uut).registerUpdateListener(configArgumentCaptor.capture());
            UpdateListenerConfig capturedUL = configArgumentCaptor.getValue();
            context.assertEquals(ID, capturedUL.getId());
            context.assertEquals(STATEMENT_NAME, capturedUL.getStatementName());
            context.assertEquals(TARGET, capturedUL.getTarget());

            context.assertEquals(1, uut.getStatementToConsumerMap().size());
            context.assertTrue(uut.getStatementToConsumerMap().containsKey(STATEMENT_NAME));
            context.assertEquals(LOCAL_STATEMENT_ADDRESS, uut.getStatementToConsumerMap().get(STATEMENT_NAME).address());
            async.complete();
        });
    }

    @Test
    public void test_exceptionInRegisterMethodFailsRegistration(TestContext context) {
        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);

        doThrow(new IllegalArgumentException("Some Exception")).when(uut).registerUpdateListener(any(UpdateListenerConfig.class));

        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertTrue(reply.failed());
            async.complete();
        });
    }

    @Test
    public void test_registerConsumerDoesFailWithInsufficientConfig(TestContext context) {
        Async async = context.async();

        final JsonObject copyConfig = UL_CONFIG.copy();
        copyConfig.remove("target");
        JsonObject registerMessage = new JsonObject().put("register", copyConfig);

        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertTrue(reply.failed());
            async.complete();
        });
    }

    @Test
    public void test_unregisterConsumerUnregistersConsumer(TestContext context) {
        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);


        // register consumer
        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertEquals(1, uut.getStatementToConsumerMap().size());

            JsonObject unregisterMessage = new JsonObject().put("unregister", UL_CONFIG);

            //unregister consumer
            vertx.eventBus().request(CONSUMER_ADDRESS, unregisterMessage, reply2 -> {
                context.assertEquals(0, uut.getStatementToConsumerMap().size());
                async.complete();
            });
        });
    }

    @Test
    public void test_unregisterConsumerDoesNotUnregistersConsumerIfMultipleListenersForStatement(TestContext context) {
        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);
        JsonObject registerMessage2 = new JsonObject().put("register", UL_CONFIG_SAME_STATEMENT_OTHER_TARGET);
        JsonObject unregisterMessage = new JsonObject().put("unregister", UL_CONFIG);



        // register consumer
        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertEquals(1, uut.getStatementToConsumerMap().size());
            context.assertEquals(1, uut.getStatementToListenerMap().get(STATEMENT_NAME).size());
            context.assertTrue(uut.getStatementToListenerMap().get(STATEMENT_NAME).contains(UL_CONFIG_OBJECT));

            vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage2, reply2 -> {
                context.assertEquals(1, uut.getStatementToConsumerMap().size());
                context.assertEquals(2, uut.getStatementToListenerMap().get(STATEMENT_NAME).size());
                context.assertTrue(uut.getStatementToListenerMap().get(STATEMENT_NAME).contains(UL_CONFIG_OBJECT));
                context.assertTrue(uut.getStatementToListenerMap().get(STATEMENT_NAME).contains(UL_CONFIG_SAME_STATEMENT_OBJECT));
                //unregister consumer
                vertx.eventBus().request(CONSUMER_ADDRESS, unregisterMessage, reply3 -> {
                    context.assertEquals(1, uut.getStatementToConsumerMap().size());
                    context.assertTrue(uut.getStatementToConsumerMap().containsKey(STATEMENT_NAME));
                    context.assertEquals(LOCAL_STATEMENT_ADDRESS, uut.getStatementToConsumerMap().get(STATEMENT_NAME).address());

                    context.assertEquals(1, uut.getStatementToListenerMap().get(STATEMENT_NAME).size());
                    context.assertTrue(
                            uut.getStatementToListenerMap().get(STATEMENT_NAME).contains(UL_CONFIG_SAME_STATEMENT_OBJECT));

                    async.complete();
                });

            });
        });
    }

    @Test
    public void test_unregisterMessageCallsUnregisterMethod(TestContext context) {

        Async async = context.async();
        JsonObject registerMessage = new JsonObject().put("register", UL_CONFIG);
        JsonObject unregisterMessage = new JsonObject().put("unregister", UL_CONFIG);
        ArgumentCaptor<UpdateListenerConfig> configArgumentCaptor = ArgumentCaptor.forClass(UpdateListenerConfig.class);

        vertx.eventBus().request(CONSUMER_ADDRESS, registerMessage, reply -> {
            context.assertTrue(reply.succeeded());

            vertx.eventBus().request(CONSUMER_ADDRESS, unregisterMessage, reply2 -> {
                verify(uut).unregisterUpdateListener(configArgumentCaptor.capture());
                UpdateListenerConfig capturedUL = configArgumentCaptor.getValue();
                context.assertEquals(ID, capturedUL.getId());
                context.assertEquals(STATEMENT_NAME, capturedUL.getStatementName());
                context.assertEquals(TARGET, capturedUL.getTarget());

                async.complete();
            });
        });
    }

    @Test
    public void test_reset(TestContext context) {
        Async async = context.async();
        vertx.eventBus().request(CONSUMER_ADDRESS, new JsonObject().put("reset", "reset"), reply -> {
            context.assertTrue(reply.succeeded());
            context.assertEquals("register command executed", ((JsonObject)reply.result().body()).getString("status"));
            async.complete();
        });
    }


    @After
    public void cleanUp(TestContext context) {
        if (deploymentId != null) {
            Async async = context.async();
            vertx.undeploy(deploymentId, result -> {
                vertx.close();
                async.complete();
            });
        }
    }

}
