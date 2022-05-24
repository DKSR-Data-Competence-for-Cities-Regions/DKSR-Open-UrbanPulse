package de.urbanpulse.dist.outbound;

import de.urbanpulse.dist.outbound.client.HttpVerticle;

import de.urbanpulse.dist.outbound.server.ws.WsPublisherVerticle;
import de.urbanpulse.dist.outbound.server.ws.WsServerVerticle;
import de.urbanpulse.transfer.CommandResult;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.TransferStructureFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class OutboundCommandHandlerTest {

    Vertx vertx;
    @Mock
    MainVerticle mainVerticle;

    OutboundCommandHandler uut;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        vertx = Vertx.vertx();
        when(mainVerticle.getVertx()).thenReturn(vertx);

        uut = new OutboundCommandHandler(mainVerticle);
    }

    @Test
    public void test_registerHttpUpdateListenerProducesValidUndoCommand(TestContext context) {
        final String target = "https://example.com/target";
        setupHttpTests(context);
        registerUndoCommandTestHelper(target, context);
    }

    @Test
    public void test_unregisterHttpUpdateListenerProducesValidUndoCommand(TestContext context) {
        final String target = "https://example.com/target";
        setupHttpTests(context);
        unregisterUndoCommandTestHelper(target, context);
    }

    @Test
    public void test_registerWebsocketUpdateListenerProducesValidUndoCommand(TestContext context) {
        final String target = "wss://example.com/target";
        setupWsTests(false, false);
        registerUndoCommandTestHelper(target, context);
    }

    @Test
    public void test_unregisterWebsocketUpdateListenerProducesValidUndoCommand(TestContext context) {
        final String target = "wss://example.com/target";
        setupWsTests(false, false);
        unregisterUndoCommandTestHelper(target, context);
    }


    @Test
    public void test_failingWsResponseCancelsULRegistration(TestContext context){
        final String target = "wss://example.com/target";
        setupWsTests(true, false);
        registerAbortsWithFailingResponseTestHelper(context, target);
    }

    @Test
    public void test_failingWsResponseCancelsULUnregistration(TestContext context){
        final String target = "wss://example.com/target";
        setupWsTests(false, true);
        unregisterAbortsWithFailingResponseTestHelper(context, target);
    }

      @Test
    public void test_URIStartsWithAnEmptyChar(TestContext context) {
        final String target = " wss://example.com/target";
        setupWsTests(false, false);
        registerUndoCommandTestHelper(target, context);
    }

    @Test
    public void test_URIStartsWithAnInvalidChar(TestContext context) {
        final String target = "$%wss://example.com/target";
        setupWsTests(false, false);
        registerAbortsWithFailingResponseTestHelper(context, target);
    }

    @Test
    public void test_URIIsNull(TestContext context) {
        final String target = null;
        setupWsTests(false, false);
        registerAbortsWithFailingResponseTestHelper(context, target);
    }

    @Test
    public void test_URIIsIncorrect(TestContext context) {
        final String target = "wss://example.com/target$%&/";
        setupWsTests(false, false);
        registerAbortsWithFailingResponseTestHelper(context, target);
    }

    @Test
    public void test_CleanTargetOfEmptySpaces(){
        Assert.assertEquals("https://ss.com",uut.cleanTargetOfEmptySpaces("   https://ss.com"));
        Assert.assertEquals("https://ss.com",uut.cleanTargetOfEmptySpaces("https://ss.com"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_cleanTargetOfNullAndEmtpy(){
        Assert.assertNull(uut.cleanTargetOfEmptySpaces("    "));
        Assert.assertNull(uut.cleanTargetOfEmptySpaces(null));
    }

    /**
     * Generic register undo command test for all update listener types.
     *
     * @param target  the target of the update listener
     * @param context the test context to wait for async methods
     */
    private void registerUndoCommandTestHelper(String target, TestContext context) {
        Async async = context.async();

        final String id = "1";
        final String statementName = "MyTestStatement";
        Map<String, Object> config = generateUpdateListenerConfig(id, target, statementName);

        // Result callback of the unregister call
        CommandResult undoResult = (result, undo) -> {
            context.assertNull(uut.getListeners(statementName), "Listener got not unregistered");
            async.complete();
        };

        // Result callback of the initial register call
        CommandResult commandResult = (result, undo) -> {
            context.assertNotNull(undo, "No UndoCommand created though it was requested");

            // undo command is valid
            context.assertEquals("unregisterUpdateListener", undo.getMethodName());
            Map<String, Object> undoArgs = undo.getArgs();
            context.assertEquals(config.get("id"), undoArgs.get("id"));

            // listener got registered
            context.assertTrue(uut.getListeners(statementName).contains(id), "Listener got not registered");

            undo.execute(undoResult);
        };

        uut.registerUpdateListener(config, true, commandResult);

        async.await();
    }

    /**
     * Generic unregister undo command test for all update listener types.
     *
     * @param target  the target of the update listener
     * @param context the test context to wait for async methods
     */
    private void unregisterUndoCommandTestHelper(String target, TestContext context) {
        Async initialRegisterAsync = context.async();
        Async undoExecuted = context.async();

        final String id = "1";
        final String statementName = "MyTestStatement";
        Map<String, Object> config = generateUpdateListenerConfig(id, target, statementName);

        CommandResult registerResult = (r, u) -> initialRegisterAsync.complete();

        uut.registerUpdateListener(config, false, registerResult);

        initialRegisterAsync.await();

        CommandResult undoResult = (result, undo) -> {
            undoExecuted.complete();
        };
        CommandResult unregisterResult = (result, undo) -> {
            context.assertNotNull(undo, "No undo command was created");
            context.assertEquals("registerUpdateListener", undo.getMethodName());
            context.assertEquals(config, undo.getArgs(), "Different listener in UndoCommand than initially created");

            context.assertNull(uut.getListeners(statementName), "Listener still registered after unregister");

            undo.execute(undoResult);
        };
        uut.unregisterUpdateListener(generateUpdateListenerConfig(id, null, null), true, unregisterResult);

        undoExecuted.await();

        context.assertTrue(uut.getListeners(statementName).contains(id), "Listener got not re-registered");
    }

    private void registerAbortsWithFailingResponseTestHelper(TestContext context, String target) {
        Async async = context.async();

        final String id = "1";
        final String statementName = "MyTestStatement";
        Map<String, Object> config = generateUpdateListenerConfig(id, target, statementName);


        // Result callback of the initial register call
        CommandResult commandResult = (result, undo) -> {
            // check if the result is an error
            context.assertTrue(result.containsKey(TransferStructureFactory.TAG_BODY));
            JsonObject body = result.getJsonObject(TransferStructureFactory.TAG_BODY);
            context.assertTrue(body.containsKey(ErrorFactory.ERROR_MESSAGE_TAG));
            async.complete();
        };

        uut.registerUpdateListener(config, false, commandResult);

        async.await();
    }

    private void unregisterAbortsWithFailingResponseTestHelper(TestContext context, String target) {
        Async initialRegisterAsync = context.async();
        Async async = context.async();

        final String id = "1";
        final String statementName = "MyTestStatement";
        Map<String, Object> config = generateUpdateListenerConfig(id, target, statementName);

        CommandResult registerResult = (r, u) -> initialRegisterAsync.complete();

        uut.registerUpdateListener(config, false, registerResult);

        initialRegisterAsync.await();

        CommandResult unregisterResult = (result, undo) -> {
            // check if the result is an error
            context.assertTrue(result.containsKey(TransferStructureFactory.TAG_BODY));
            JsonObject body = result.getJsonObject(TransferStructureFactory.TAG_BODY);
            context.assertTrue(body.containsKey(ErrorFactory.ERROR_MESSAGE_TAG));
            async.complete();
        };
        uut.unregisterUpdateListener(generateUpdateListenerConfig(id, null, null), false, unregisterResult);

        async.await();
    }

    /**
     * Sets up mock answers for methods used by the OutboundCommandHandler during http update listener
     * registration and unregistration.
     *
     * @param context the test context to wait for async methods
     */
    private void setupHttpTests(TestContext context) {

        when(mainVerticle.config()).thenReturn(new JsonObject());

        List<String> deploymentIds = new LinkedList<>();

        Answer<Void> deploymentAnswer = new Answer<Void>() {
            int count; // used to make the deployment ids unique

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Handler<AsyncResult<String>> handler = invocation.getArgument(3, Handler.class);
                Future<String> deploymentResult = Future.succeededFuture("" + count);
                deploymentIds.add("" + count);
                count++;
                handler.handle(deploymentResult);
                return null;
            }
        };

        final Answer<Void> undeploymentAnswer = invocation -> {
            String deploymentId = invocation.getArgument(0, String.class);
            Handler<AsyncResult<Void>> handler = invocation.getArgument(1, Handler.class);
            context.assertTrue(deploymentIds.contains(deploymentId),
                    "Outbound command handler tried to unregister non existing verticle");
            Future<Void> undeploymentResult = Future.succeededFuture(null);
            handler.handle(undeploymentResult);
            return null;
        };

        doAnswer(deploymentAnswer).when(mainVerticle)
                .deployWorkerVerticle(eq(HttpVerticle.class.getName()), any(), anyInt(), any());
        doAnswer(undeploymentAnswer).when(mainVerticle).undeployVerticle(anyString(), any());
    }


    private void setupWsTests(boolean registerFailing, boolean unregisterFailing) {
        // reply to register messages to not block execution
        vertx.eventBus().localConsumer(WsServerVerticle.SETUP_ADDRESS, getMockHandler(registerFailing, unregisterFailing));
        vertx.eventBus().localConsumer(WsPublisherVerticle.SETUP_ADDRESS, getMockHandler(registerFailing, unregisterFailing));
    }

    private Handler<Message<Object>> getMockHandler(boolean registerFailing, boolean unregisterFailing){
        return m -> {
            JsonObject body = (JsonObject) m.body();
            if (body.containsKey("register")) {
                if (registerFailing) {
                    m.fail(500, "Failure");
                } else {
                    m.reply(null);
                }
            } else if (body.containsKey("unregister")) {
                if (unregisterFailing) {
                    m.fail(500, "Failure");
                } else {
                    m.reply(null);
                }
            }
            m.reply(null);
        };
    }


    private Map<String, Object> generateUpdateListenerConfig(String id, String target, String statementName) {
        Map<String, Object> config = new HashMap<>();

        config.put("id", id);
        if (target != null) {
            config.put("target", target);
        }
        if (statementName != null) {
            config.put("statementName", statementName);
        }

        return config;

    }


}
