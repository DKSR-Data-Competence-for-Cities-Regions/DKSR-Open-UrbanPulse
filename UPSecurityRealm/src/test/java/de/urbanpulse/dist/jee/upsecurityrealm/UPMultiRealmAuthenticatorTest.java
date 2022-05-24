package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHMACSecurityRealm;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UPMultiRealmAuthenticatorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private UPMultiRealmAuthenticator upMultiRealmAuthenticator;

    @Mock
    private UPSecurityRealm upSecurityRealm;

    @Mock
    private UPHMACSecurityRealm uphmacSecurityRealm;

    @Mock
    private AtLeastOneSuccessfulStrategy mockAuthenticationStrategy;

    @Mock
    private AuthenticationInfo mockAuthenticationInfo;

    @Mock
    private AuthenticationInfo mockAggregateAuthenticationInfo;

    @Mock
    private PrincipalCollection mockPrincipalCollection;


    private UsernamePasswordToken usernamePasswordToken;
    private Collection<Realm> realms;

    @Before
    public void init() {
        upMultiRealmAuthenticator = spy(UPMultiRealmAuthenticator.class);
        usernamePasswordToken = new UsernamePasswordToken("dummy", "dummy");
        realms = new HashSet<>();
        realms.add(upSecurityRealm);
        realms.add(uphmacSecurityRealm);
        reset(upSecurityRealm, uphmacSecurityRealm, mockAuthenticationInfo, mockAuthenticationStrategy,
                mockPrincipalCollection, mockAggregateAuthenticationInfo);
        trainMocks();
    }

    @Test
    public void doMultiRealmAuthenticationTestWithOneSupportedToken() {
        when(uphmacSecurityRealm.supports(any())).thenReturn(false);
        when(upSecurityRealm.supports(any())).thenReturn(true);
        when(upSecurityRealm.getAuthenticationInfo(usernamePasswordToken)).thenReturn(mockAuthenticationInfo);
        when(mockAuthenticationStrategy.afterAttempt(any(), any(), any(), any(), any())).thenReturn(mockAggregateAuthenticationInfo);
        when(mockAggregateAuthenticationInfo.getPrincipals()).thenReturn(mockPrincipalCollection);

        AuthenticationInfo authenticationInfo = upMultiRealmAuthenticator.doMultiRealmAuthentication(realms, usernamePasswordToken);

        assertNotNull(authenticationInfo);
        verify(mockAuthenticationStrategy, times(1)).beforeAllAttempts(any(), any());
        verify(mockAuthenticationStrategy, times(2)).beforeAttempt(any(), any(), any());
        verify(mockAuthenticationStrategy, times(1)).afterAttempt(any(), any(), any(), any(), any());
        verify(mockAuthenticationStrategy, times(1)).afterAllAttempts(any(), any());
    }

    @Test
    public void doMultiRealmAuthenticationTestWithoutSupportedToken() {
        exception.expect(AuthenticationException.class);
        when(uphmacSecurityRealm.supports(any())).thenReturn(false);
        when(upSecurityRealm.supports(any())).thenReturn(false);

        upMultiRealmAuthenticator.doMultiRealmAuthentication(realms, usernamePasswordToken);
    }

    private void trainMocks() {
        when(upMultiRealmAuthenticator.getAuthenticationStrategy()).thenReturn(mockAuthenticationStrategy);
        when(mockAuthenticationStrategy.beforeAllAttempts(any(), any())).thenReturn(mockAggregateAuthenticationInfo);
        when(mockAuthenticationStrategy.beforeAttempt(any(), any(), any())).thenCallRealMethod();
        when(mockAuthenticationStrategy.afterAllAttempts(any(), any())).thenCallRealMethod();
    }

}
