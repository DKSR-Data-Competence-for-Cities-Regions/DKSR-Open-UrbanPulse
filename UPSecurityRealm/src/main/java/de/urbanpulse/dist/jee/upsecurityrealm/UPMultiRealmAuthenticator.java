package de.urbanpulse.dist.jee.upsecurityrealm;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.AuthenticationStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This authenticator overrides multi realm authentication so that exceptions are not caught within the authenticator class.
 * Therefore, not only AuthenticationException is thrown in the AtLeastOneSuccessfulStrategy
 * if something in case of something goes wrong.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPMultiRealmAuthenticator extends ModularRealmAuthenticator {

    private static final Logger LOG = Logger.getLogger(UPMultiRealmAuthenticator.class.getName());

    @Override
    public AuthenticationInfo doMultiRealmAuthentication(Collection<Realm> realms, AuthenticationToken token) {
        AuthenticationStrategy strategy = this.getAuthenticationStrategy();
        AuthenticationInfo aggregate = strategy.beforeAllAttempts(realms, token);
        LOG.info("Iterating through " + realms.size() + " realms for PAM authentication");

        Iterator realmIterator = realms.iterator();

        while (realmIterator.hasNext()) {
            Realm realm = (Realm) realmIterator.next();
            aggregate = strategy.beforeAttempt(realm, token, aggregate);
            if (realm.supports(token)) {
                LOG.info("Attempting to authenticate token using realm: " + realm.getName());
                AuthenticationInfo info = realm.getAuthenticationInfo(token);
                LOG.info("Got authentication info from token");
                aggregate = strategy.afterAttempt(realm, token, info, aggregate, null);
            } else {
                LOG.info("Realm " + realm + " does not support token " + token + ".  Skipping realm.");
            }
        }

        LOG.info("Calling after all attempts");
        return strategy.afterAllAttempts(token, aggregate);
    }


}
