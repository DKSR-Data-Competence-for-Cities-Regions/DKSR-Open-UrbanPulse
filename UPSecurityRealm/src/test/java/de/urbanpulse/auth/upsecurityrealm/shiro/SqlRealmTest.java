package de.urbanpulse.auth.upsecurityrealm.shiro;

import org.apache.shiro.authc.credential.PasswordMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SqlRealmTest {

    @Mock
    private DataSource dataSource;

    private SqlRealm sqlRealm;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_init_withCredentialsAndName(){
        sqlRealm = new SqlRealm(dataSource,true,"testMe");
        assertTrue(sqlRealm.getCredentialsMatcher() instanceof PasswordMatcher);
        assertEquals("testMe",sqlRealm.getName());
    }

    @Test
    public void test_init_noCredentials(){
        sqlRealm = new SqlRealm(dataSource,false,"testMe");
        assertFalse(sqlRealm.getCredentialsMatcher() instanceof PasswordMatcher);
    }
}
