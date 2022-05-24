package de.urbanpulse.dist.outbound.server.auth;

import io.vertx.core.*;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadState;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ShiroAuthHandlerTest {

    private static ShiroAuthHandler authHandler;
    private static Vertx mockedVertx;
    private static ThreadState threadState;
    private static SecurityManager manager;
    private static Subject mockedSubject;

    private RoutingContext mockRoutingContext;
    private User mockedUser;

    @BeforeClass
    public static void initShiro() {
        manager = Mockito.mock(SecurityManager.class);
        mockedVertx = Mockito.mock(Vertx.class);
        mockedSubject = Mockito.mock(Subject.class);
        SecurityUtils.setSecurityManager(manager);
        Context context = Mockito.mock(Context.class);
        JsonObject config = Mockito.mock(JsonObject.class);
        when(mockedVertx.getOrCreateContext()).thenReturn(context);
        when(context.config()).thenReturn(config);
        when(manager.createSubject(any())).thenReturn(mockedSubject);
        when(mockedSubject.getPrincipal()).thenReturn("foo");
        authHandler = new ShiroAuthHandler(mockedVertx);
    }

    @AfterClass
    public static void stopShiro() {
        if (threadState != null) {
            threadState.clear();
            threadState = null;
        }
        try {
            org.apache.shiro.mgt.SecurityManager securityManager = (org.apache.shiro.mgt.SecurityManager) SecurityUtils.getSecurityManager();
            LifecycleUtils.destroy(securityManager);
        } catch (UnavailableSecurityManagerException e) {

        }
        SecurityUtils.setSecurityManager(null);
    }

    @After
    public void cleanUp() {
        if (threadState != null) {
            threadState.clear();
            threadState = null;
        }
    }

    @Before
    public void init() {
        if (threadState != null) {
            threadState.clear();
            threadState = null;
        }
        threadState = new SubjectThreadState(mockedSubject);
        threadState.bind();
        mockRoutingContext = Mockito.mock(RoutingContext.class);
        mockedUser = Mockito.mock(User.class);
    }

    @Test
    public void test_authenticate_valid_credentials() {
        HttpServerRequest mockHttpServerRequest = initServerRequestMock();
        initMultiMapMock(mockHttpServerRequest);
        initIsAuthenticatedtMock(Future.succeededFuture(mockedUser));

        authHandler.authenticate(mockRoutingContext);
        verify(mockRoutingContext, times(1)).setUser(mockedUser);
    }

    @Test
    public void test_authenticate_invalid_credentials() {
        HttpServerRequest mockHttpServerRequest = initServerRequestMock();
        initMultiMapMock(mockHttpServerRequest);
        initServerResponseMock(mockHttpServerRequest);
        initIsAuthenticatedtMock(Future.failedFuture("Invalid credentials."));

        authHandler.authenticate(mockRoutingContext);
        verify(mockRoutingContext, times(0)).setUser(any());
    }

    @Test
    public void test_authenticate_failure() {
        HttpServerRequest mockHttpServerRequest = initServerRequestMock();
        initMultiMapMock(mockHttpServerRequest);
        initServerResponseMock(mockHttpServerRequest);
        initIsAuthenticatedtMock(Future.succeededFuture(null));

        authHandler.authenticate(mockRoutingContext);
        verify(mockRoutingContext, times(0)).setUser(any());
    }

    @Test
    public void test_authorize_true() {
        String requiredPermission = "validPermission";
        initUserIsAuthorizedMock(requiredPermission, Future.succeededFuture(true));

        authHandler.authorize(mockRoutingContext, requiredPermission);
        verify(mockRoutingContext, times(1)).next();
    }

    @Test
    public void test_authorize_false() {
        String requiredPermission = "invalidPermission";
        initUserIsAuthorizedMock(requiredPermission, Future.succeededFuture(false));

        HttpServerRequest mockHttpServerRequest = initServerRequestMock();
        initServerResponseMock(mockHttpServerRequest);

        authHandler.authorize(mockRoutingContext, "invalidPermission");
        verify(mockRoutingContext, times(0)).next();
    }

    @Test
    public void test_authorize_failure() {
        String requiredPermission = "willFail";
        initUserIsAuthorizedMock(requiredPermission, Future.failedFuture("Failed on purpose!"));

        HttpServerRequest mockHttpServerRequest = initServerRequestMock();
        initServerResponseMock(mockHttpServerRequest);

        authHandler.authorize(mockRoutingContext, "willFail");
        verify(mockRoutingContext, times(0)).next();
    }

    private HttpServerRequest initServerRequestMock() {
        HttpServerRequest mockHttpServerRequest = Mockito.mock(HttpServerRequest.class);
        when(mockRoutingContext.request()).thenReturn(mockHttpServerRequest);
        return mockHttpServerRequest;
    }

    private void initServerResponseMock(HttpServerRequest mockHttpServerRequest) {
        HttpServerResponse mockHttpServerResponse = Mockito.mock(HttpServerResponse.class);
        when(mockHttpServerRequest.response()).thenReturn(mockHttpServerResponse);
        when(mockHttpServerResponse.setStatusCode(anyInt())).thenReturn(mockHttpServerResponse);
        when(mockHttpServerResponse.putHeader(anyString(), anyString())).thenReturn(mockHttpServerResponse);
    }

    private void initMultiMapMock(HttpServerRequest mockHttpServerRequest) {
        MultiMap mockedMultiMap = Mockito.mock(MultiMap.class);
        when(mockHttpServerRequest.headers()).thenReturn(mockedMultiMap);
        when(mockedMultiMap.get(HttpHeaders.AUTHORIZATION)).thenReturn("Authorization: Basic Zm9vOmJhcgo=");
    }

    private void initIsAuthenticatedtMock(Future future) {
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.<Handler<Future<User>>>getArgument(0).handle(Future.future());
            invocationOnMock.<Handler<AsyncResult<User>>>getArgument(1).handle(future);
            return null;
        }).when(mockedVertx).executeBlocking(any(Handler.class), any(Handler.class));
    }

    private void initUserIsAuthorizedMock(String requiredPermission, Future future) {
        when(mockRoutingContext.user()).thenReturn(mockedUser);
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.<Handler<AsyncResult<Boolean>>>getArgument(1).handle(future);
            return null;
        }).when(mockedUser).isAuthorized(eq(requiredPermission), any());
    }

}
