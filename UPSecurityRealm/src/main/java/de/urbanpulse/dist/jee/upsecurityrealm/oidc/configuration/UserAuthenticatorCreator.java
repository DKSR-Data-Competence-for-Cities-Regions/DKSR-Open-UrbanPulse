package de.urbanpulse.dist.jee.upsecurityrealm.oidc.configuration;

import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

public class UserAuthenticatorCreator {

    private final KeyCloakConfigLookUp keyCloakConfigLookUp;

    public UserAuthenticatorCreator() {
        keyCloakConfigLookUp = new KeyCloakConfigLookUp();
    }

    public UserInfoOidcAuthenticator createUserInfoAuthenticator() {
        KeycloakOidcConfiguration keycloakConfig = new KeycloakOidcConfiguration();
        keycloakConfig.setClientId(keyCloakConfigLookUp.getKeyCloakClientId());
        keycloakConfig.setSecret(keyCloakConfigLookUp.getKeyCloakSecret());
        keycloakConfig.setRealm(keyCloakConfigLookUp.getKeyCloakRealm());
        keycloakConfig.setBaseUri(keyCloakConfigLookUp.getKeyCloakBaseUri());

        return new UserInfoOidcAuthenticator(keycloakConfig);
    }
}
