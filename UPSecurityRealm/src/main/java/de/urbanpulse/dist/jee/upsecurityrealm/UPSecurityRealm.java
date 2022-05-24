package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.helpers.UserManagementDAOInitializer;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.HmacToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * Security realm for Basic Auth
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPSecurityRealm extends AuthorizingRealm {

    private Logger LOGGER = Logger.getLogger(UPSecurityRealm.class.getName());
    private SecurityRealmDAO userManagementDAO;
    private static final String REALM_NAME = "UPSecurityRealm";

    public UPSecurityRealm() {
        super();

        // Needs to be disabled, otherwise after changing a user's password they won't be able to login.
        //Pending: Clear the cache manually after changing user.
        setAuthenticationCachingEnabled(Boolean.FALSE);

        try {
            this.userManagementDAO = UserManagementDAOInitializer.getUserManagementDAOFromJNDIs(new InitialContext());
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "Cannot do the JNDI Lookup to instantiate the UserManagementDAO : {0}", ex);
        }

    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return (token instanceof UsernamePasswordToken) && !(token instanceof HmacToken);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        if (!UsernamePasswordToken.class.equals(token.getClass())) {
            return null;
        }
        // identify account to log to
        UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
        String username = userPassToken.getUsername();

        if (username == null) {
            LOGGER.warning("Username is null.");
            return null;
        }
        UserEntity user = userManagementDAO.getUserbyName(username);
        if (user == null) {
            LOGGER.log(Level.WARNING, "No account found for user [{0}]", username);
            throw new UnknownAccountException("No account found for user " + username);
        }
        LoginToken loginToken = new LoginToken(UPAuthMode.BASIC, String.valueOf(user.getId()));
        SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection(loginToken, REALM_NAME);
        return new SimpleAuthenticationInfo(simplePrincipalCollection, user.getPasswordHash());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //null usernames are invalid
        if (principals == null) {
            LOGGER.severe("Principal collection is null");
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }
        Collection principalCollection = principals.fromRealm(REALM_NAME);
        if (principalCollection.isEmpty()) {
            return null;
        }
        LoginToken loginToken = (LoginToken) principalCollection.iterator().next();
        if (!loginToken.getAuthmode().equals(UPAuthMode.BASIC)) {
            return null;
        }

        String subjectId = loginToken.getSubjectId();
        UserEntity subject = userManagementDAO.getUserById(subjectId);
        SimpleAuthorizationInfo simpleAuthorizationInfo = SimpleAuthorizationInfoFactory.createSimpleAuthorizationInfo(subject);

        return simpleAuthorizationInfo;
    }

}
