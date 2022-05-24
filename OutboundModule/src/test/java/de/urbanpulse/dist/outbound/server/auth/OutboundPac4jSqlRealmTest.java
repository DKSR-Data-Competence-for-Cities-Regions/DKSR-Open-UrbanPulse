package de.urbanpulse.dist.outbound.server.auth;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.HmacToken;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundPac4jSqlRealmTest {

    @Mock
    private UserInfoOidcAuthenticator userInfoOidcAuthenticator;



    @Mock
    private PrincipalCollection principalCollectionMock;

    @Mock
    private BasicDataSource dataSource;

    @Mock
    private Connection mockedConnection;

    @Mock
    private PreparedStatement mockedPrepareStatement;

    @Mock
    private ResultSet mockedResultSet;

    private OutboundPac4jSqlRealm outboundPac4jSqlRealm;

    @Before
    public void init() throws IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        outboundPac4jSqlRealm = new OutboundPac4jSqlRealm(dataSource,false,
                userInfoOidcAuthenticator,"http://localhost");
        Field declaredFields[] = outboundPac4jSqlRealm.getClass().getDeclaredFields();
        for (Field field : declaredFields){
            if (field.getName().contains("pac4jSecurityHelper")){
                field.setAccessible(true);

            }
        }
    }

    @Test
    public void test_doGetAuthorizationInfo_success() throws SQLException {
        Collection<Object> tempCollection = new ArrayList<>();
        tempCollection.add(new TokenCredentials("test"));
        tempCollection.add("me");

        Set<String> roles = new HashSet<>();
        roles.add("user");


        Mockito.when(principalCollectionMock.fromRealm(Mockito.anyString())).thenReturn(tempCollection);

        Mockito.doReturn(mockedConnection).when(dataSource).getConnection();
        Mockito.doReturn(mockedPrepareStatement).when(mockedConnection).prepareStatement(Mockito.anyString());
        Mockito.doReturn(mockedResultSet).when(mockedPrepareStatement).executeQuery();
        Mockito.doReturn(true).doReturn(false).when(mockedResultSet).next();
        Mockito.doReturn("me2").when(mockedResultSet).getString(Mockito.anyInt());

        AuthorizationInfo authZInfo = outboundPac4jSqlRealm.doGetAuthorizationInfo(principalCollectionMock);
        assertNotNull(authZInfo);
        assertEquals(2, authZInfo.getRoles().size());
    }

    @Test
    public void test_doGetAuthorizationInfo_emptyPrincipal(){
        Collection<Object> tempCollection = new ArrayList<>();

        Mockito.when(principalCollectionMock.fromRealm(Mockito.anyString())).thenReturn(tempCollection);

        AuthorizationInfo authZInfo = outboundPac4jSqlRealm.doGetAuthorizationInfo(principalCollectionMock);
        assertNull(authZInfo);
    }

    @Test
    public void test_doGetAuthorizationInfo_singlePrincipal(){
        Collection<Object> tempCollection = new ArrayList<>();
        tempCollection.add("meh");

        Mockito.when(principalCollectionMock.fromRealm(Mockito.anyString())).thenReturn(tempCollection);

        AuthorizationInfo authZInfo = outboundPac4jSqlRealm.doGetAuthorizationInfo(principalCollectionMock);
        assertNull(authZInfo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_doGetAuthorizationInfo_failing() throws SQLException {
        Collection<Object> tempCollection = new ArrayList<>();
        tempCollection.add(new TokenCredentials("test"));
        tempCollection.add("me");

        Set<String> roles = new HashSet<>();
        roles.add("user");


        Mockito.when(principalCollectionMock.fromRealm(Mockito.anyString())).thenReturn(tempCollection);

        Mockito.doReturn(mockedConnection).when(dataSource).getConnection();
        Mockito.doThrow(new IllegalArgumentException("THE WAY IS SHUT1!")).when(mockedConnection).prepareStatement(Mockito.anyString());

        outboundPac4jSqlRealm.doGetAuthorizationInfo(principalCollectionMock);
    }



}
