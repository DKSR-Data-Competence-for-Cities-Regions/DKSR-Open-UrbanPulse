/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.HmacToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections.collection.CompositeCollection;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UPSecurityRealmTest {

    private static final String REALM_NAME = "UPSecurityRealm";

    private UsernamePasswordToken usernamePasswordToken;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private SecurityRealmDAO securityRealmDAO;

    @Mock
    private Iterator iteratorMock;

    @Mock
    private PrincipalCollection principalCollectionMock;

    @Mock
    private Collection collectionMock;

    @InjectMocks
    private UPSecurityRealm uPSecurityRealm;

    public UPSecurityRealmTest() {
    }

    /**
     * Test of supports method, of class UPSecurityRealm.
     */
    @Test
    public void testSupports() {
        boolean result = uPSecurityRealm.supports(new AuthenticationToken() {
            @Override
            public Object getPrincipal() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Object getCredentials() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        assertEquals(false, result);
        result = uPSecurityRealm.supports(new UsernamePasswordToken());
        assertEquals(true, result);
        result = uPSecurityRealm.supports(new HmacToken(null, null, null, null));
        assertEquals(false, result);
    }

    /**
     * Test of doGetAuthenticationInfo method, of class UPSecurityRealm.
     */
    @Test
    public void testDoGetAuthenticationInfo() {
        usernamePasswordToken = new UsernamePasswordToken("admin", "");
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        when(securityRealmDAO.getUserbyName("admin")).thenReturn(user);


        SimpleAuthenticationInfo authInfo = (SimpleAuthenticationInfo) uPSecurityRealm.doGetAuthenticationInfo(usernamePasswordToken);
        assertTrue(authInfo.getPrincipals().getPrimaryPrincipal() instanceof LoginToken);
    }

    /**
     * Test of doGetAuthorizationInfo method, of class UPSecurityRealm.
     */

    @Test
    public void testDoGetAuthorizationInfo() {
        Collection<LoginToken> tempCollection = new ArrayList<>();
        String userId = UUID.randomUUID().toString();
        LoginToken loginToken = new LoginToken(UPAuthMode.BASIC, userId);
        tempCollection.add(loginToken);

        List<RoleEntity> roleList = new ArrayList<>();
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName("admin");
        roleList.add(roleEntity);

        UserEntity user = new UserEntity();
        user.setRoles(roleList);

        when(principalCollectionMock.fromRealm(REALM_NAME)).thenReturn(tempCollection);
        when(securityRealmDAO.getUserById(userId)).thenReturn(user);

        AuthorizationInfo authorizationInfo = uPSecurityRealm.doGetAuthorizationInfo(principalCollectionMock);
        assertTrue(authorizationInfo.getRoles().contains("admin"));
    }

    @Test
    public void testAuthenticationReturnsNullWithWrongToken() {
        //Creating incorrect token object
        HmacToken token = new HmacToken(null, null, null, null);
        AuthenticationInfo authenticationInfo = uPSecurityRealm.doGetAuthenticationInfo(token);
        assertNull(authenticationInfo);
    }

    @Test
    public void testAuthenticationReturnsNullWithNullUsername() {
        usernamePasswordToken = new UsernamePasswordToken();
        usernamePasswordToken.setUsername(null);

        AuthenticationInfo authenticationInfo = uPSecurityRealm.doGetAuthenticationInfo(usernamePasswordToken);
        assertNull(authenticationInfo);
    }

    @Test
    public void testCorrectExceptionThrownWhenAccountNotFound() {
        exception.expect(UnknownAccountException.class);

        usernamePasswordToken = new UsernamePasswordToken();
        usernamePasswordToken.setUsername("dummy");

        uPSecurityRealm.doGetAuthenticationInfo(usernamePasswordToken);
    }

    @Test
    public void testCorrectExceptionWhenPrincipalsAreNull() {
        exception.expect(AuthorizationException.class);

        principalCollectionMock = null;
        uPSecurityRealm.doGetAuthorizationInfo(principalCollectionMock);
    }

    @Test
    public void testReturnNullWhenFromRealmResultIsEmpty() {
        when(principalCollectionMock.fromRealm(anyString())).thenReturn(new CompositeCollection());
        assertNull(uPSecurityRealm.doGetAuthorizationInfo(principalCollectionMock));
    }

    @Test
    public void testReturnNullWhenWrongTokenIsFound() {
        LoginToken token = new LoginToken(UPAuthMode.UPCONNECTOR, UUID.randomUUID().toString());

        when(principalCollectionMock.fromRealm(anyString())).thenReturn(collectionMock);
        when(collectionMock.isEmpty()).thenReturn(false);
        when(collectionMock.iterator()).thenReturn(iteratorMock);
        when(iteratorMock.next()).thenReturn(token);


        AuthorizationInfo authorizationInfo = uPSecurityRealm.doGetAuthorizationInfo(principalCollectionMock);
        assertNull(authorizationInfo);
    }
}
