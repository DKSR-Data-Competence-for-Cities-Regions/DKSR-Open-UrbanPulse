package de.urbanpulse.urbanpulsemanagement.shiro;

import de.urbanpulse.dist.jee.upsecurityrealm.UPMultiRealmAuthenticator;
import de.urbanpulse.dist.jee.upsecurityrealm.UPPasswordService;
import de.urbanpulse.dist.jee.upsecurityrealm.UPSecurityRealm;
import de.urbanpulse.dist.jee.upsecurityrealm.cors.CorsFilter;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.BodyWrapperFilter;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHMACCredentialMatcher;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHMACSecurityRealm;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHmacAuthenticationFilter;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.filter.mgt.*;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.Collection;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Dependent
public class ShiroConfiguration {

    private static final String CORS_FILTER_NAME = "cors";
    private static final String BODY_WRAPPER_FILTER_NAME = "body";
    private static final String ANONYMUS_FILTER_NAME = "anon";
    private static final String HMAC_FILTER_NAME = "hmac";

    @Produces
    public WebSecurityManager getSecurityManager() {
        AuthorizingRealm pwRealm = new UPSecurityRealm();



        CredentialsMatcher credentialsMatcher = new PasswordMatcher();
        ((PasswordMatcher) credentialsMatcher).setPasswordService(new UPPasswordService());
        pwRealm.setCredentialsMatcher(credentialsMatcher);

        //There seems to be no point in adding a credentials matcher for oidc tokens.
        //Also after extending the logic we need a credentials matcher that does
        //nothing otherwise the flow gets broken. We get one token and another AuthenticationInfo for it.


        AuthorizingRealm hmacRealm = new UPHMACSecurityRealm();
        hmacRealm.setCredentialsMatcher(new UPHMACCredentialMatcher());

        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

        Collection<Realm> realms = new ArrayList<>();

        realms.add(pwRealm);
        realms.add(hmacRealm);
        securityManager.setRealms(realms);

        UPMultiRealmAuthenticator upMultiRealmAuthenticator = new UPMultiRealmAuthenticator();
        upMultiRealmAuthenticator.setRealms(realms);
        securityManager.setAuthenticator(upMultiRealmAuthenticator);

        CacheManager cacheManager = new EhCacheManager();
        ((EhCacheManager) cacheManager).setCacheManagerConfigFile("classpath:ehcache.xml");
        securityManager.setCacheManager(cacheManager);


        return securityManager;
    }

    @Produces
    public FilterChainResolver getFilterChainResolver() {
        FilterChainManager filterChainManager = new DefaultFilterChainManager();

        filterChainManager.addFilter(CORS_FILTER_NAME, new CorsFilter());
        filterChainManager.addFilter(BODY_WRAPPER_FILTER_NAME, new BodyWrapperFilter());
        filterChainManager.addFilter(ANONYMUS_FILTER_NAME, new AnonymousFilter());
        filterChainManager.addFilter(HMAC_FILTER_NAME, new UPHmacAuthenticationFilter());

        filterChainManager.createChain("/api/swagger.json", CORS_FILTER_NAME);
        filterChainManager.addToChain("/api/swagger.json", ANONYMUS_FILTER_NAME);
        filterChainManager.createChain("/api/swagger.yaml", CORS_FILTER_NAME);
        filterChainManager.addToChain("/api/swagger.yaml", ANONYMUS_FILTER_NAME);

        filterChainManager.createChain("/status/**", HMAC_FILTER_NAME);
        filterChainManager.addToChain("/status/**", DefaultFilter.roles.name(), "admin");

        filterChainManager.createChain("/api/login", CORS_FILTER_NAME);
        filterChainManager.addToChain("/api/login", ANONYMUS_FILTER_NAME);

        filterChainManager.createChain("/**", CORS_FILTER_NAME);
        filterChainManager.addToChain("/**", BODY_WRAPPER_FILTER_NAME);
        filterChainManager.addToChain("/**", HMAC_FILTER_NAME);


        PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
        resolver.setFilterChainManager(filterChainManager);

        return resolver;
    }

}
