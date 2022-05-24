package de.urbanpulse.auth.upsecurityrealm.shiro;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Base64;
import org.apache.shiro.authc.AuthenticationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
public class UpShiroAuthTest {

    private static final String USERNAME = "President Skroob";
    private static final String PASSWORD = "12345"; // Amazing, I have the same combination on my luggage!

    private Vertx vertx;

    @Mock
    private SecurityManager securityManager;

    @Mock
    private Subject subject;

    private UpShiroAuth upShiroAuth;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();

        initMocks(this);
        given(securityManager.createSubject(any())).willReturn(subject);
        given(subject.getPrincipal()).willReturn(new JsonObject().put("username", USERNAME));

        upShiroAuth = new UpShiroAuth(vertx, securityManager);
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void test_authenticate_withJsonObject_success(TestContext context) {
        Async async = context.async();
        JsonObject authInfo = new JsonObject().put("username", USERNAME)
                .put("password", PASSWORD);
        upShiroAuth.authenticate(authInfo, result -> {
            context.assertTrue(result.succeeded());
            context.assertTrue(result.result() instanceof UpShiroUser);
            async.complete();
        });
    }

    @Test
    public void test_authenticate_withJsonObject_failure(TestContext context) {
        Async async = context.async();
        doThrow(new AuthenticationException("Dr. Schlotkin says no."))
                .when(subject).login(any());
        JsonObject authInfo = new JsonObject().put("username", USERNAME)
                .put("password", PASSWORD);
        upShiroAuth.authenticate(authInfo, result -> {
            context.assertFalse(result.succeeded());
            async.complete();
        });
    }

    @Test
    public void test_authenticate_withBasicAuth_success(TestContext context) {
        Async async = context.async();
        String authInfo = "Basic " + Base64.getEncoder().encodeToString((USERNAME.toLowerCase() + ":" + PASSWORD).getBytes());
        upShiroAuth.authenticate(authInfo, result -> {
            context.assertTrue(result.succeeded());
            context.assertTrue(result.result() instanceof UpShiroUser);
            async.complete();
        });
    }

    @Test
    public void test_authenticate_withBasicAuth_failure(TestContext context) {
        Async async = context.async();
        doThrow(new AuthenticationException("Dr. Schlotkin says no."))
                .when(subject).login(any());
        String authInfo = "Basic " + Base64.getEncoder().encodeToString((USERNAME.toLowerCase() + ":" + PASSWORD).getBytes());
        upShiroAuth.authenticate(authInfo, result -> {
            context.assertFalse(result.succeeded());
            async.complete();
        });

    }

    @Test
    public void test_authenticate_withBearerToken_success(TestContext context){
        Async async = context.async();
        String bearerToken = "Bearer token1233";
        upShiroAuth.authenticate(bearerToken, result -> {
            context.assertTrue(result.succeeded());
            context.assertTrue(result.result() instanceof UpShiroUser);
            async.complete();
        });
    }

    @Test
    public void test_authenticate_withBearerToken_failure(TestContext context){
        Async async = context.async();
        doThrow(new AuthenticationException("Dr. Schlotkin says no."))
                .when(subject).login(any());
        String bearerToken = "Bearer token1233";
        upShiroAuth.authenticate(bearerToken, result -> {
            context.assertTrue(result.failed());
            async.complete();
        });
    }
}
