package de.urbanpulse.transfer.vertx;

import de.urbanpulse.transfer.CommandHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import de.urbanpulse.transfer.ConnectionHandler;
import de.urbanpulse.transfer.ErrorFactory;
import de.urbanpulse.transfer.TransferStructureFactory;
import static de.urbanpulse.transfer.TransferStructureFactory.COMMAND_HEARTBEAT;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractMainVerticleMockitoTest {

    @InjectMocks
    private final MainVerticleImpl mainVerticle = new MainVerticleImpl();

    @Mock
    private ConnectionHandler connectionHandler;

    @Mock
    private CircuitBreaker breaker;

    @Mock
    private Vertx vertx;

    @Test
    public void test_registerAtServer_should_call_requestSetupFromServer() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("registerAtServer");
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put("id", "theID")
                .put(TAG_BODY, new JsonObject()
                        .put("some setup", 1000))
                .put(TransferStructureFactory.TAG_HEADER, new JsonObject());

        doAnswer((InvocationOnMock invocation) -> {
            Handler<JsonObject> callback = invocation.getArgument(4, Handler.class);
            callback.handle(message);
            return null;
        })
                .when(connectionHandler)
                .sendCommand(eq("sm_address"), eq("register"), any(), anyLong(), any());

        method.invoke(mainVerticle);
        verify(connectionHandler, times(1)).setConnectionId(eq("theID"), any());
        verify(vertx, times(0)).setTimer(anyLong(), any());
    }

    @Test
    public void test_registerAtServer_should_trigger_start_timer_for_reset_on_error() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("registerAtServer");
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put(TransferStructureFactory.TAG_HEADER, new JsonObject()
                        .put(ErrorFactory.ERROR_CODE_TAG, 23))
                .put(TAG_BODY, new JsonObject()
                        .put(ErrorFactory.ERROR_MESSAGE_TAG, "some error"));

        doAnswer((InvocationOnMock invocation) -> {
            Handler<JsonObject> callback = invocation.getArgument(4, Handler.class);
            callback.handle(message);
            return null;
        })
                .when(connectionHandler)
                .sendCommand(eq("sm_address"), eq("register"), any(), anyLong(), any());

        method.invoke(mainVerticle);
        verify(connectionHandler, times(0)).setConnectionId(eq("theID"), any());
        verify(vertx, times(1)).setTimer(anyLong(), any());
    }

    /**
     * Test that the setup fails if error received from the server.
     *
     * @throws NoSuchMethodException method could not be found
     * @throws IllegalAccessException the currently executing method does not
     * have access to the definition of the specified class, field, method or
     * constructor.
     * @throws IllegalArgumentException input is incorrect or missing.
     * @throws InvocationTargetException reflection error
     */
    @Test
    public void testMessageWithErrorCodeFailsSetup() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("requestSetupFromUPManagement", Promise.class);
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put(TransferStructureFactory.TAG_HEADER, new JsonObject()
                        .put(ErrorFactory.ERROR_CODE_TAG, 23))
                .put(TAG_BODY, new JsonObject()
                        .put(ErrorFactory.ERROR_MESSAGE_TAG, "some error"));

        doAnswer((InvocationOnMock invocation) -> {
            Handler<JsonObject> callback = invocation.getArgument(4, Handler.class);
            callback.handle(message);
            return null;
        })
                .when(connectionHandler)
                .sendCommand(eq("sm_address"), eq("sendSetup"), any(), anyLong(), any());

        Promise promise = Promise.promise();

        method.invoke(mainVerticle, (Promise) promise);
        Assert.assertTrue(promise.future().failed());
    }

    /**
     * Test that valid message that is received from the server does not fail.
     *
     * @throws NoSuchMethodException method could not be found
     * @throws IllegalAccessException the currently executing method does not
     * have access to the definition of the specified class, field, method or
     * constructor.
     * @throws IllegalArgumentException input is incorrect or missing.
     * @throws InvocationTargetException reflection error
     */
    @Test
    public void testMessageWithoutErrorCodeNotFailsSetup() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("requestSetupFromUPManagement", Promise.class);
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put(TAG_BODY, new JsonObject()
                        .put("some setup", 1000)
                        .put(COMMAND_HEARTBEAT, 1000));

        doAnswer((InvocationOnMock invocation) -> {
            Handler<JsonObject> callback = invocation.getArgument(4, Handler.class);
            callback.handle(message);
            return null;
        })
                .when(connectionHandler)
                .sendCommand(eq("sm_address"), eq("sendSetup"), any(), anyLong(), any());

        Promise promise = Promise.promise();

        method.invoke(mainVerticle, (Promise) promise);
        Assert.assertTrue(promise.future().succeeded());
    }

    @Test
    public void handleSetupMessage_should_start_HeartBeat() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("handleSetupMessage", JsonObject.class);
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put(TAG_BODY, new JsonObject()
                        .put(COMMAND_HEARTBEAT, 1000));

        doReturn(1L).when(vertx).setPeriodic(anyLong(), any());

        Future<JsonObject> result = (Future<JsonObject>) method.invoke(mainVerticle, message);
        verify(vertx, times(1)).setPeriodic(anyLong(), any());
        Assert.assertTrue(result.succeeded());
    }

    @Test
    public void calling_reset_when_registering_in_progress_should_be_postponed() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

        mainVerticle.resetConnection();
        verify(vertx).setTimer(anyLong(), any());
        // cleanup
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
    }

    @Test
    public void calling_multiple_resetModule_when_registering_in_progress_only_one_should_be_postponed() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

        // pretend it is successful call
        Field resetTimerIdField = mainVerticle.getClass().getSuperclass().getDeclaredField("resetTimerId");
        resetTimerIdField.setAccessible(true);
        resetTimerIdField.set(mainVerticle, 1000L);

        mainVerticle.resetConnection();
        mainVerticle.resetConnection();
        verify(vertx, times(0)).setTimer(anyLong(), any());

        //cleanup
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
        resetTimerIdField.set(mainVerticle, 1000L);
    }

    @Test
    public void test_handleSetupResponse_sets_isRegistering_false_when_suceeded() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("handleSetupResponse", AsyncResult.class);
        method.setAccessible(true);


        JsonObject message = new JsonObject()
                .put(TAG_BODY, new JsonObject()
                        .put(COMMAND_HEARTBEAT, 1000));

        AsyncResult<JsonObject> asyncResult = Future.succeededFuture(message);

        method.invoke(mainVerticle, asyncResult);

        // no reset timer is set
        verify(vertx, times(0)).setTimer(anyLong(), any());
        // no reset is called
        verify(connectionHandler, times(0)).reset();
        // registering is finished
        Assert.assertFalse(((AtomicBoolean) isRegisteringField.get(mainVerticle)).get());

        // cleanup
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
    }

    @Test
    public void test_handleSetupResponse_let_isRegistering_true_when_failed_and_requestSetupFromServer() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("handleSetupResponse", AsyncResult.class);
        method.setAccessible(true);


        AsyncResult<JsonObject> asyncResult = Future.failedFuture("Failed for testing.");

        method.invoke(mainVerticle, asyncResult);

        // one requestSetup timer is set
        verify(vertx, times(1)).setTimer(anyLong(), any());
        // no reset is called
        verify(connectionHandler, times(0)).reset();
        // registering is finished
        Assert.assertTrue(((AtomicBoolean) isRegisteringField.get(mainVerticle)).get());

        // cleanup
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
    }

    @Test
    public void test_handleSetupResponse_calls_resetConnection_maxOpenCircuit() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(true);

        Field openCircuitCounterField = mainVerticle.getClass().getSuperclass().getDeclaredField("openCircuitCounter");
        openCircuitCounterField.setAccessible(true);
        ((AtomicInteger) openCircuitCounterField.get(mainVerticle)).getAndSet(2);

        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("handleSetupResponse", AsyncResult.class);
        method.setAccessible(true);


        AsyncResult<JsonObject> asyncResult = Future.failedFuture("Failed for testing.");

        method.invoke(mainVerticle, asyncResult);

        // no reset timer is set
        verify(vertx, times(0)).setTimer(anyLong(), any());
        // reset is called once. This means calling restConnection was successful
        verify(connectionHandler, times(1)).reset();
        // registering is again in progress
        Assert.assertTrue(((AtomicBoolean) isRegisteringField.get(mainVerticle)).get());

        // cleanup
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);
        ((AtomicInteger) openCircuitCounterField.get(mainVerticle)).getAndSet(0);
    }

    @Test
    public void test_registerAtServer_sets_isRegistering_false_when_failed() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Field isRegisteringField = mainVerticle.getClass().getSuperclass().getDeclaredField("isRegistering");
        isRegisteringField.setAccessible(true);
        ((AtomicBoolean) isRegisteringField.get(mainVerticle)).getAndSet(false);

        Method method = mainVerticle.getClass().getSuperclass().getDeclaredMethod("registerAtServer");
        method.setAccessible(true);

        JsonObject message = new JsonObject()
                .put(TransferStructureFactory.TAG_HEADER, new JsonObject()
                        .put(ErrorFactory.ERROR_CODE_TAG, 23))
                .put(TAG_BODY, new JsonObject()
                        .put(ErrorFactory.ERROR_MESSAGE_TAG, "some error"));

        doAnswer((InvocationOnMock invocation) -> {
            Handler<JsonObject> callback = invocation.getArgument(4, Handler.class);
            callback.handle(message);
            return null;
        })
                .when(connectionHandler)
                .sendCommand(eq("sm_address"), eq("register"), any(), anyLong(), any());

        method.invoke(mainVerticle);

        // didn't set connection
        verify(connectionHandler, times(0)).setConnectionId(eq("theID"), any());
        // registered one reset connection timer
        verify(vertx, times(1)).setTimer(anyLong(), any());
        // registering is finished, so reset connection can be performed
        Assert.assertFalse(((AtomicBoolean) isRegisteringField.get(mainVerticle)).get());
    }

    public class MainVerticleImpl extends AbstractMainVerticle {

        @Override
        protected CommandHandler createCommandHandler() {
            return null;
        }

        @Override
        protected void resetModule(Handler<Void> callback) {
            callback.handle(null);
        }

        @Override
        protected void setupModule(JsonObject setup, Handler<AsyncResult<Void>> setupResultHandler) {
            setupResultHandler.handle(Future.succeededFuture());
        }

        @Override
        protected Map<String, Object> createRegisterModuleConfig() {
            return Collections.emptyMap();
        }

    }
}
