package de.urbanpulse.dist.jee.upsecurityrealm.helpers;

import de.urbanpulse.dist.jee.upsecurityrealm.SecurityRealmDAO;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UserManagementDAOInitializer {

    private static final Logger LOGGER = Logger.getLogger(UserManagementDAOInitializer.class.getName());

    private UserManagementDAOInitializer(){

    }

    public static SecurityRealmDAO getUserManagementDAOFromJNDIs(InitialContext cx) throws NamingException {
        SecurityRealmDAO userManagementDAO;
        String moduleName = (String) cx.lookup("java:module/ModuleName");
        String applicationName = (String) cx.lookup("java:app/AppName");

        String lookup = "java:global" + ((applicationName.equals(moduleName)) ? "" : "/" + applicationName) + "/" + moduleName + "/SecurityRealmDAO";
        LOGGER.log(Level.INFO, "lookup for SecurityRealmDAO: {0}", lookup);

        userManagementDAO = (SecurityRealmDAO) cx.lookup(String.format(lookup));

        return userManagementDAO;
    }
}
