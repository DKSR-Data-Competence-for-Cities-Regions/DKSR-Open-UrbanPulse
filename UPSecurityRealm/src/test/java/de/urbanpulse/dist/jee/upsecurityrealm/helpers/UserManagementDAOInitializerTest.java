package de.urbanpulse.dist.jee.upsecurityrealm.helpers;

import de.urbanpulse.dist.jee.upsecurityrealm.SecurityRealmDAO;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UserManagementDAOInitializerTest {

    @Test(expected = NamingException.class)
    public void test_emptyContext() throws NamingException {
        InitialContext context = Mockito.mock(InitialContext.class);

        Mockito.doThrow(NamingException.class).when(context).lookup(Mockito.anyString());
        UserManagementDAOInitializer.getUserManagementDAOFromJNDIs(context);
    }

    @Test
    public void test_success() throws NamingException {
        InitialContext context = Mockito.mock(InitialContext.class);
        SecurityRealmDAO securityRealmDAO = Mockito.mock(SecurityRealmDAO.class);

        Mockito.doReturn("test").when(context).lookup("java:module/ModuleName");
        Mockito.doReturn("test2").when(context).lookup("java:app/AppName");
        Mockito.doReturn(securityRealmDAO).when(context).lookup("java:global/test2/test/SecurityRealmDAO");
        SecurityRealmDAO responseDAO = UserManagementDAOInitializer.getUserManagementDAOFromJNDIs(context);

        assertNotNull(responseDAO);
        assertEquals(securityRealmDAO,responseDAO);
    }
}
