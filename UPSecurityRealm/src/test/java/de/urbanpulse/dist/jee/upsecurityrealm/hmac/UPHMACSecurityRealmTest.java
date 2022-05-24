package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.SecurityRealmDAO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.*;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

@RunWith(MockitoJUnitRunner.class)
public class UPHMACSecurityRealmTest {

    private String recentString;
    private RoleEntity roleEntity;
    private PermissionEntity permissionEntity;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private SecurityRealmDAO securityRealmDAO;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private PrincipalCollection principalCollection;

    @Mock
    private HmacToken hmacTokenMock;

    @Mock
    private UserEntity userEntity;

    @InjectMocks
    private UPHMACSecurityRealm uphmacSecurityRealm;

    @Before
    public void initTimestamp() {
        //Generating valid timestamp, because it is not the subject of the current test
        DateTime recent = DateTime.now(DateTimeZone.UTC).minusMinutes(14).minusSeconds(59);
        recentString = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").print(recent);
    }

    @Test
    public void testAuthenticationWithIncorrectToken() {
        AuthenticationInfo authenticationInfo = uphmacSecurityRealm.doGetAuthenticationInfo(new UsernamePasswordToken());
        assertNull(authenticationInfo);
    }

    @Test
    public void testAuthenticationWithNullUsername() {
        AuthenticationInfo authenticationInfo = uphmacSecurityRealm.doGetAuthenticationInfo(hmacTokenMock);
        assertNull(authenticationInfo);
    }

    @Test
    public void testUPAuthModeWithIncorrectCredentials() {
        exception.expect(IncorrectCredentialsException.class);

        //Imitating that the user is not found so an exception can be thrown
        when(securityRealmDAO.getUserbyName(any())).thenReturn(null);
        when(servletRequest.getHeader(anyString())).thenReturn(recentString);

        HmacToken hmacToken = new HmacToken("dummy", "dummy", servletRequest, UPAuthMode.UP);
        uphmacSecurityRealm.doGetAuthenticationInfo(hmacToken);
    }

    @Test
    public void testCONNECTORAuthModeWithIncorrectCredentials() {
        exception.expect(IncorrectCredentialsException.class);

        //Imitating that the connector user is not found so an exception can be thrown
        when(securityRealmDAO.getConnectorById(any())).thenReturn(null);
        when(servletRequest.getHeader(anyString())).thenReturn(recentString);

        HmacToken hmacToken = new HmacToken("dummy", "dummy", servletRequest, UPAuthMode.UPCONNECTOR);
        uphmacSecurityRealm.doGetAuthenticationInfo(hmacToken);
    }

    @Test
    public void testUndefinedAuthModeWithIncorrectCredentials() {
        exception.expect(IncorrectCredentialsException.class);
        HmacToken hmacToken = new HmacToken("dummy", "dummy", servletRequest, UPAuthMode.BASIC);
        uphmacSecurityRealm.doGetAuthenticationInfo(hmacToken);
    }

    //Authorization
    @Test
    public void testAuthorizationWithNullPrincipals() {
        exception.expect(AuthorizationException.class);
        uphmacSecurityRealm.doGetAuthorizationInfo(null);
    }

    @Test
    public void testAuthorizationReturnNullWhenFromRealmReturnsEmptyCollections() {
        when(principalCollection.fromRealm(anyString())).thenReturn(new ArrayList());
        AuthorizationInfo authorizationInfo = uphmacSecurityRealm.doGetAuthorizationInfo(principalCollection);
        assertNull(authorizationInfo);
    }

    @Test
    public void testAuthorizationReturnNullWhenDifferentAuthModeIsGiven() {
        //Basic auth, should trigger default case in the switch
        Collection<LoginToken> tempCollection = new ArrayList<>();
        LoginToken loginToken = new LoginToken(UPAuthMode.BASIC, UUID.randomUUID().toString());
        tempCollection.add(loginToken);

        when(principalCollection.fromRealm(anyString())).thenReturn(tempCollection);

        AuthorizationInfo authorizationInfo = uphmacSecurityRealm.doGetAuthorizationInfo(principalCollection);
        assertNull(authorizationInfo);
    }

    //Tests of expected behaviour
    @Test
    public void testSupports() {
        assertFalse(uphmacSecurityRealm.supports(new UsernamePasswordToken()));
        assertTrue(uphmacSecurityRealm.supports(new HmacToken(null, null, null, null)));
    }

    @Test
    public void testDoAuthenticationInfo() {
        //Testing UP Auth mode
        UserEntity entity = new UserEntity();
        entity.setName("admin");
        entity.setId("UUID");
        entity.setKey("secretkey");

        when(servletRequest.getHeader(anyString())).thenReturn(recentString);
        when(securityRealmDAO.getUserbyName(anyString())).thenReturn(entity);

        HmacToken hmacToken = new HmacToken("admin", "admin", servletRequest, UPAuthMode.UP);

        AuthenticationInfo upAuthenticationInfo = uphmacSecurityRealm.doGetAuthenticationInfo(hmacToken);

        assertFalse(upAuthenticationInfo.getPrincipals().isEmpty());
        assertTrue(upAuthenticationInfo.getPrincipals().iterator().next() instanceof LoginToken);

        LoginToken loginToken = (LoginToken) upAuthenticationInfo.getPrincipals().iterator().next();
        assertTrue(loginToken.getAuthmode().equals(UPAuthMode.UP));
        assertTrue(loginToken.getSubjectId().equals(entity.getId()));
        assertTrue(upAuthenticationInfo.getCredentials().equals(entity.getKey()));

        //Testing Connector Auth mode
        hmacToken.setAuthMode(UPAuthMode.UPCONNECTOR);

        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setKey("secretkey");
        connectorEntity.setId("00000000-0000-0000-0000-000000000001");

        when(securityRealmDAO.getConnectorById(anyString())).thenReturn(connectorEntity);
        AuthenticationInfo connectorAuthenticationInfo = uphmacSecurityRealm.doGetAuthenticationInfo(hmacToken);

        assertFalse(upAuthenticationInfo.getPrincipals().isEmpty());
        assertTrue(upAuthenticationInfo.getPrincipals().iterator().next() instanceof LoginToken);

        loginToken = (LoginToken) connectorAuthenticationInfo.getPrincipals().iterator().next();
        assertTrue(loginToken.getAuthmode().equals(UPAuthMode.UPCONNECTOR));
        assertTrue(loginToken.getSubjectId().equals(connectorEntity.getId().toString()));
        assertTrue(upAuthenticationInfo.getCredentials().equals(connectorEntity.getKey()));
    }

    @Test
    public void testDoGetAuthorizationInfo() {
        //Testing UP auth mode
        Collection<LoginToken> tempCollection = new ArrayList<>();
        LoginToken loginToken = new LoginToken(UPAuthMode.UP, UUID.randomUUID().toString());
        tempCollection.add(loginToken);

        roleEntity = new RoleEntity();
        roleEntity.setName("admin");

        ArrayList<RoleEntity> roles = new ArrayList<>();
        roles.add(roleEntity);

        permissionEntity = new PermissionEntity();
        permissionEntity.setName("users:read");

        ArrayList<PermissionEntity> permissions = new ArrayList<>();
        permissions.add(permissionEntity);

        when(userEntity.getRoles()).thenReturn(roles);
        when(userEntity.getPermissions()).thenReturn(permissions);
        when(principalCollection.fromRealm(anyString())).thenReturn(tempCollection);
        when(securityRealmDAO.getUserById(anyString())).thenReturn(userEntity);

        AuthorizationInfo upAuthorizationInfo = uphmacSecurityRealm.doGetAuthorizationInfo(principalCollection);
        assertFalse(upAuthorizationInfo.getRoles().isEmpty());
        assertTrue(upAuthorizationInfo.getRoles().iterator().next().equals("admin"));
        assertTrue(upAuthorizationInfo.getStringPermissions().contains("users:read"));

        //Testing Connector auth mode
        tempCollection.remove(loginToken);
        loginToken.setAuthmode(UPAuthMode.UPCONNECTOR);
        tempCollection.add(loginToken);

        when(principalCollection.fromRealm(anyString())).thenReturn(tempCollection);

        AuthorizationInfo connectorAuthorizationInfo = uphmacSecurityRealm.doGetAuthorizationInfo(principalCollection);
        assertFalse(connectorAuthorizationInfo.getRoles().isEmpty());
        assertTrue(connectorAuthorizationInfo.getRoles().iterator().next().equals("connector"));
    }
}
