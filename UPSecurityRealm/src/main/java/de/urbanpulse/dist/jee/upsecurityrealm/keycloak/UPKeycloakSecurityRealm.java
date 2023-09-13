package de.urbanpulse.dist.jee.upsecurityrealm.keycloak;

import com.nimbusds.jwt.JWTClaimsSet;
import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.SimpleAuthorizationInfoFactory;
import de.urbanpulse.dist.jee.upsecurityrealm.UPPasswordService;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.HmacToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import de.urbanpulse.dist.jee.upsecurityrealm.jwt.AccessTokenValidator;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UPKeycloakSecurityRealm extends AuthorizingRealm {

    protected static final Logger LOGGER = Logger.getLogger(UPKeycloakSecurityRealm.class.getName());
    protected static final String REALM_NAME = "UPKeycloakSecurityRealm";
    protected AuthzClient authzClient;

    private AccessTokenValidator tokenValidator;

    public UPKeycloakSecurityRealm() {
        tokenValidator = new AccessTokenValidator();
        setAuthenticationCachingEnabled(Boolean.FALSE);

        try {
            InitialContext cx = new InitialContext();
            String pathToKeycloakJson = (String) cx.lookup("keycloak/configPath");
            tryReadKeyCloakConfig(pathToKeycloakJson);
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "Cannot do the JNDI Lookup to instantiate the UserManagementDAO : {0}", ex);
        }

    }

    public void tryReadKeyCloakConfig(String pathToKeycloakJson) {
        // create a new instance based on the configuration defined in a keycloak.json located in your classpath
        // if we are here then we have something defined
        try {
            InputStream configStream = new FileInputStream(pathToKeycloakJson);
            authzClient = AuthzClient.create(configStream);
        } catch (FileNotFoundException e) {
            LOGGER.severe(e.getMessage());
            LOGGER.info("Using resource path....");
            authzClient = AuthzClient.create();
        }
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return (token instanceof UsernamePasswordToken) && !(token instanceof HmacToken);
    }

    @Override
    public AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        if (principals == null) {
            LOGGER.info("Principal collection is null");
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }

        //If this was not meant for this realm then we will skip the authorisation and do
        //nothing. According to the documentation fromRealm will return either the collection
        //off principles or an empty Collection. This is very useful for multi realm deployments.
        //As part of #19741
        Collection<Object> principalCollection = principals.fromRealm(REALM_NAME);
        if (principalCollection.isEmpty()) {
            return null;
        }
        LoginToken loginToken = (LoginToken) principalCollection.iterator().next();
        if (!loginToken.getAuthmode().equals(UPAuthMode.BASIC)) {
            LOGGER.info("LoginToken getAuthmode is not basic!!!");
            return null;
        }

        return SimpleAuthorizationInfoFactory.createSimpleAuthorizationInfoFromClaims(loginToken.getPayload());
    }

    @Override
    public AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        SimpleAuthenticationInfo simpleAuth = new SimpleAuthenticationInfo();

        if (!UsernamePasswordToken.class.equals(token.getClass())) {
            return null;
        }

        UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
        String username = userPassToken.getUsername();

        if (username == null) {
            LOGGER.warning("Username is null.");
            return null;
        }

        if (userPassToken.getPassword() == null) {
            LOGGER.warning("Password is null.");
            return null;
        }
        String password = new String(userPassToken.getPassword());

        try {
            AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken(username, password);
            JWTClaimsSet claimsSet = tokenValidator.verifyAndReturnToken(accessTokenResponse.getToken());

            if (claimsSet != null) {
                LoginToken loginToken = new LoginToken(UPAuthMode.BASIC, claimsSet.getSubject());
                loginToken.setPayload(claimsSet.getClaims());
                loginToken.setRealmName(REALM_NAME);

                SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection(loginToken, REALM_NAME);
                simpleAuth.setPrincipals(simplePrincipalCollection);
                simpleAuth.setCredentials(new UPPasswordService().encryptPassword(password));
            } else {
                LOGGER.log(Level.INFO, "jwtString size: {0}", accessTokenResponse.getToken().length());
            }
        } catch (RuntimeException e) {
            throw new IncorrectCredentialsException(e.getMessage());
        }

        return simpleAuth;
    }

}
