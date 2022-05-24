package de.urbanpulse.dist.outbound.server.auth;

import io.vertx.core.json.JsonObject;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.CachingSecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

import java.util.ArrayList;
import java.util.List;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SecurityManagerInitializer {

    private SecurityManagerInitializer() {
    }

    public static void initSecurityManager(JsonObject setup) {
        JsonObject jdbcConfig = setup.getJsonObject("jdbc");
        JsonObject keycloakConfig = setup.getJsonObject("keycloak");

        org.apache.shiro.mgt.SecurityManager securityManager; // not the java.lang.SecurityManager

        BasicDataSource dataSource = jdbcConfig.mapTo(BasicDataSource.class);

        dataSource.setValidationQuery("SELECT 1");

        // Collect realms
        List<Realm> realms = new ArrayList<>();
        Realm realm = new OutboundSqlRealm(dataSource, true,"SqlRealm");
        ((AuthorizingRealm) realm).setAuthorizationCachingEnabled(true);
        //the configuration for keycloak is optional
        //if no configuration was supplied we don't want to fail but just skip this part
        if (keycloakConfig != null) {
            OutboundPac4jSqlRealm pac4jRealm = new OutboundPac4jSqlRealm(dataSource,
                    false, getKeycloakUserInfoAuthenticator(keycloakConfig),
                    keycloakConfig.getString("urbanPulseUrl"));
            pac4jRealm.setCredentialsMatcher(new AllowAllCredentialsMatcher());
            realms.add(pac4jRealm);
        }
        realms.add(realm);

        //Initialize security manager with realms
        securityManager = new DefaultSecurityManager(realms);

        //Set shiro caching mechanism
        CacheManager cacheManager = new MemoryConstrainedCacheManager();
        ((CachingSecurityManager) securityManager).setCacheManager(cacheManager);

        SecurityUtils.setSecurityManager(securityManager);
    }

    private static UserInfoOidcAuthenticator getKeycloakUserInfoAuthenticator(JsonObject keycloakConfig){
        KeycloakOidcConfiguration keycloakOidcConfiguration = new KeycloakOidcConfiguration();
        keycloakOidcConfiguration.setClientId(keycloakConfig.getString("clientId","urbanpulse"));
        keycloakOidcConfiguration.setSecret(keycloakConfig.getString("secret","123456789123456789"));
        keycloakOidcConfiguration.setRealm(keycloakConfig.getString("realm","ui"));
        keycloakOidcConfiguration.setBaseUri(keycloakConfig.getString("apiBaseUrl","http://localhost:9080/auth"));

        return new UserInfoOidcAuthenticator(keycloakOidcConfiguration);
    }

}
