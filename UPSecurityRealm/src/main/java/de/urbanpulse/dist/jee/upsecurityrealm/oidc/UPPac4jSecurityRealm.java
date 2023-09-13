package de.urbanpulse.dist.jee.upsecurityrealm.oidc;

import de.urbanpulse.dist.jee.upsecurityrealm.helpers.UserManagementDAOInitializer;
import de.urbanpulse.dist.jee.upsecurityrealm.oidc.helpers.Pac4jSecurityHelper;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.SecurityRealmDAO;
import de.urbanpulse.dist.jee.upsecurityrealm.SimpleAuthorizationInfoFactory;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class UPPac4jSecurityRealm extends AuthorizingRealm {

    private static final String REALM_NAME = "UPPac4jSecurityRealm";
    private static final Logger LOGGER = Logger.getLogger(UPPac4jSecurityRealm.class.getName());

    private Pac4jSecurityHelper pac4jSecurityHelper;
    private UserInfoOidcAuthenticator userInfoOidcAuthenticator;
    private SecurityRealmDAO userManagementDAO;

    public UPPac4jSecurityRealm(UserInfoOidcAuthenticator userInfoOidcAuthenticator) {
        super();
        //override the default token used for this realm so that shiro can call it with the expected token
        setAuthenticationTokenClass(OidcToken.class);
        this.userInfoOidcAuthenticator = userInfoOidcAuthenticator;
        pac4jSecurityHelper = new Pac4jSecurityHelper();
        initUserManagementDAO();
    }

    private void initUserManagementDAO() {
        try {
            this.userManagementDAO = UserManagementDAOInitializer.getUserManagementDAOFromJNDIs(new InitialContext());
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "Cannot do the JNDI Lookup to instantiate the UserManagementDAO!", ex);
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo simpleAuthorizationInfo = null;

        Object[] principalCollection = principals.fromRealm(REALM_NAME).toArray();

        if (principalCollection.length <= 0) {
            return null;
        }

        TokenCredentials tokenCredentials = (TokenCredentials) principalCollection[0];

        Set<String> userRolesFromToken = pac4jSecurityHelper.createOidcProfileAndExtractRolesFromToken(tokenCredentials);
        UserEntity userEntity = userManagementDAO.getUserbyName((String) principalCollection[1]);

        simpleAuthorizationInfo = SimpleAuthorizationInfoFactory.createSimpleAuthorizationInfo(userEntity);
        simpleAuthorizationInfo.addRoles(userRolesFromToken);

        return simpleAuthorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token){
        return pac4jSecurityHelper.createAuthInfoAndValidateToken(
                pac4jSecurityHelper.createTokenCredentialsFromShiroAuthToken(token, "http://localhost:8080/UrbanPulseManagement/api/login"),
                REALM_NAME, userInfoOidcAuthenticator);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return pac4jSecurityHelper.isTokenSupported(token);
    }
}
