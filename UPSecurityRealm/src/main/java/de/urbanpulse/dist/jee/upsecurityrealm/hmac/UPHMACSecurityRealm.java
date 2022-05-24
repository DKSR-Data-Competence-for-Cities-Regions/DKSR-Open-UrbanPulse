package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.SecurityRealmDAO;
import de.urbanpulse.dist.jee.upsecurityrealm.SimpleAuthorizationInfoFactory;
import static de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHmacAuthenticationFilter.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.web.util.WebUtils;

/**
 * A secutrity realm to handle UP HMAC authentication and authorization
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

public class UPHMACSecurityRealm extends AuthorizingRealm {

    private static final Logger LOGGER = Logger.getLogger(UPHMACSecurityRealm.class.getName());
    private static final String REALM_NAME = "UPHMACSecurityRealm";
    private SecurityRealmDAO securityRealmDAO;

    public UPHMACSecurityRealm() {
        super();

        setAuthenticationCachingEnabled(Boolean.FALSE);

        try {
            InitialContext cx = new InitialContext();
            String moduleName = (String) cx.lookup("java:module/ModuleName");
            String applicationName = (String) cx.lookup("java:app/AppName");

            String lookup = "java:global" + ((applicationName.equals(moduleName)) ? "" : "/" + applicationName) + "/" + moduleName + "/SecurityRealmDAO";
            LOGGER.log(Level.INFO, "lookup for SecurityRealmDAO: {0}", lookup);

            this.securityRealmDAO = (SecurityRealmDAO) cx.lookup(String.format(lookup));
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "Cannot do the JNDI Lookup to instantiate the UserManagementDAO : {0}", ex);
        }

    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof HmacToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        if (!(HmacToken.class.equals(token.getClass()))) {
            return null;
        }
        HmacToken hmacToken = (HmacToken) token;

        String subjectId = Base64.decodeToString((String) hmacToken.getPrincipal());
        if (subjectId == null) {
            LOGGER.warning("Username is null.");
            return null;
        }

        HttpServletRequest httpServletRequest = WebUtils.toHttp(hmacToken.getRequest());
        if (UPUSER_AUTH_SCHEMA.equals(((HmacToken) token).getAuthMode().name().toUpperCase()) ||
                UPCONNECTOR_AUTH_SCHEMA.equals(((HmacToken) token).getAuthMode().name().toUpperCase())) {

            MessageTimestampValidator.validateTimeStamp(httpServletRequest.getHeader(UP_TIMESTAMP_HEADER));
        }

        String hmacKey;
        LoginToken loginToken;
        switch (hmacToken.getAuthMode()) {
            case UP: {
                UserEntity user = securityRealmDAO.getUserbyName(subjectId);
                if (user == null) {
                    throw new IncorrectCredentialsException("could not find user: " + subjectId);
                }
                hmacKey = user.getKey();
                loginToken = new LoginToken(UPAuthMode.UP, String.valueOf(user.getId()));
                loginToken.setRealmName(REALM_NAME);
                break;
            }
            case UPCONNECTOR: {
                ConnectorEntity connector = securityRealmDAO.getConnectorById(subjectId);
                if (connector == null) {
                    throw new IncorrectCredentialsException("could not find connector: " + subjectId);
                }
                hmacKey = connector.getKey();
                loginToken = new LoginToken(UPAuthMode.UPCONNECTOR, String.valueOf(connector.getId()));
                loginToken.setRealmName(REALM_NAME);
                break;
            }
            default:
                throw new IncorrectCredentialsException("unrecognized auth method: " + hmacToken.getAuthMode());
        }

        SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection(loginToken, REALM_NAME);
        return new SimpleAuthenticationInfo(simplePrincipalCollection, hmacKey);
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
        HashSet<String> roleNames = new HashSet<>();

        switch (loginToken.getAuthmode()) {
            case UP: {
                String id = loginToken.getSubjectId();
                UserEntity subject = securityRealmDAO.getUserById(id);
                SimpleAuthorizationInfo simpleAuthorizationInfo = SimpleAuthorizationInfoFactory.createSimpleAuthorizationInfo(subject);
                return simpleAuthorizationInfo;
            }
            case UPCONNECTOR: {
                roleNames.add("connector");//Permissions handled by @RolesRequired annotation
                SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo(roleNames);
                Set<String> permissions = new HashSet<>();
                simpleAuthorizationInfo.setStringPermissions(permissions);
                return simpleAuthorizationInfo;
            }
            default: {
                return null;
            }
        }
    }

}
