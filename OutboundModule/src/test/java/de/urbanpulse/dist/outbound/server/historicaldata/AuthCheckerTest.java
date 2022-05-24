
package de.urbanpulse.dist.outbound.server.historicaldata;

import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_EVENTTYPE_HISTORICDATA_READ_TEMPLATE;
import static de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler.PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(VertxUnitRunner.class)
@SuppressWarnings("deprecation")
public class AuthCheckerTest {

    private AuthChecker authChecker;
    private User user;

    @Before
    public void setUp() {
        user = mock(User.class);

        JsonObject rulesConfigs = new JsonObject()
          .put("YouRule", new JsonObject()
            // Not a field that is actually in use, only for identification of the rule
            .put("RuleName", "YouRule"))
          .put("default_rule", new JsonObject()
            .put("RuleName", "DefaultRule"));
        JsonObject rolesToRules = new JsonObject()
          .put("testRole", "YouRule");

        authChecker = new AuthChecker(rulesConfigs, rolesToRules);
    }

    @Test
    public void testCheckRoleWillReturnRuleIfFound(TestContext context) {
        Async async = context.async();
        doAnswer((Answer<Void>) invocation -> {
            String authority = invocation.getArgument(0);
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture("roles:testRole".equals(authority)));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        authChecker.checkRole(user).onComplete(res -> {
            context.assertTrue(res.succeeded());
            JsonObject rule = res.result();
            context.assertNotNull(rule);
            context.assertEquals("YouRule", rule.getString("RuleName"));
            async.complete();
        });
    }

    @Test
    public void testCheckRoleWillReturnDefaultRuleOtherwise(TestContext context) {
        Async async = context.async();
        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture(false));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        authChecker.checkRole(user).onComplete(res -> {
            context.assertTrue(res.succeeded());
            JsonObject rule = res.result();
            context.assertNotNull(rule);
            context.assertEquals("DefaultRule", rule.getString("RuleName"));
            async.complete();
        });
    }

    @Test
    public void testFilterPermittedSIDsAndEventTypeWillReturnPermittedOnly(TestContext context) {
        Async async = context.async();
        doAnswer((Answer<Void>) invocation -> {
            String authority = invocation.getArgument(0);
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture(String.format(PERMISSION_SENSOR_SID_HISTORICDATA_READ_TEMPLATE, "A").equals(authority)));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        List<String> requestedSIDs = Arrays.asList("A", "B");

        RoutingContext routingContext = mock(RoutingContext.class);
        given(routingContext.user()).willReturn(user);

        authChecker.filterPermittedSIDsAndEventType(requestedSIDs, routingContext, "test" ,res -> {
            context.assertTrue(res.succeeded());
            List<String> permittedSIDs = res.result();
            context.assertEquals(1, permittedSIDs.size());
            context.assertEquals("A", permittedSIDs.get(0));
            async.complete();
        });
    }

    @Test
    public void testFilterPermittedSIDsAdnEventTypeWillReturnAllSIDs(TestContext context) {
        Async async = context.async();
        doAnswer((Answer<Void>) invocation -> {
            String authority = invocation.getArgument(0);
            Handler<AsyncResult<Boolean>> callback = invocation.getArgument(1);
            callback.handle(Future.succeededFuture(String.format(PERMISSION_EVENTTYPE_HISTORICDATA_READ_TEMPLATE, "test").equals(authority)));
            return null;
        }).when(user).isAuthorized(anyString(), any(Handler.class));

        List<String> requestedSIDs = Arrays.asList("A", "B");

        RoutingContext routingContext = mock(RoutingContext.class);
        given(routingContext.user()).willReturn(user);

        authChecker.filterPermittedSIDsAndEventType(requestedSIDs, routingContext, "test",res -> {
            context.assertTrue(res.succeeded());
            List<String> permittedSIDs = res.result();
            context.assertEquals(2, permittedSIDs.size());
            context.assertEquals("A", permittedSIDs.get(0));
            context.assertEquals("B", permittedSIDs.get(1));
            async.complete();
        });
    }

    @Test
    public void testEmptyRequestWillReturnForPermittedSIDs(TestContext context) {
        Async async = context.async();
        RoutingContext routingContext = mock(RoutingContext.class);
        given(routingContext.user()).willReturn(user);

        authChecker.filterPermittedSIDs(Collections.emptyList(), routingContext, res -> {
            context.assertTrue(res.succeeded());
            context.assertTrue(res.result().isEmpty());
            async.complete();
        });
    }

    @Test
    public void testEmptyRequestWillReturnForPermittedSIDsAndEventType(TestContext context) {
        Async async = context.async();
        RoutingContext routingContext = mock(RoutingContext.class);
        given(routingContext.user()).willReturn(user);

        authChecker.filterPermittedSIDsAndEventType(Collections.emptyList(), routingContext,"test", res -> {
            context.assertTrue(res.succeeded());
            context.assertTrue(res.result().isEmpty());
            async.complete();
        });
    }
}
