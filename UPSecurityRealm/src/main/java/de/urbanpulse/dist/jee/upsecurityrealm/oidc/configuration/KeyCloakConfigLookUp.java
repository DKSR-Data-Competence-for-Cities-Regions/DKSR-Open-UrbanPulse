package de.urbanpulse.dist.jee.upsecurityrealm.oidc.configuration;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyCloakConfigLookUp {

    private static final Logger LOG = Logger.getLogger(KeyCloakConfigLookUp.class.getName());

    private String keyCloakClientId;

    private String keyCloakSecret;

    private String keyCloakRealm;

    private String keyCloakBaseUri;

    private InitialContext initialContext;

    public KeyCloakConfigLookUp() {
        readKeyCloakConfigurationFromJndiVariables();
    }

    private void readKeyCloakConfigurationFromJndiVariables() {
        try {
            initialContext = new InitialContext();
        } catch (NamingException e) {
            LOG.severe(e.getMessage());
        }
        keyCloakClientId = checkLookupResult("keycloak/clientId", "urbanpulse");
        keyCloakSecret = checkLookupResult("keycloak/secret", "secret");
        keyCloakRealm = checkLookupResult("keycloak/realm", "ui");
        keyCloakBaseUri = checkLookupResult("keycloak/baseUri", "http://localhost:9080/auth");
    }

    private String checkLookupResult(String jndiName, String defaultValue) {
        try {
            return (String) initialContext.lookup(jndiName);
        } catch (NamingException ex) {
            LOG.log(Level.INFO, "Lookup for {0} failed using default {1}", new Object[]{jndiName, defaultValue});
            return defaultValue;
        }
    }

    public String getKeyCloakClientId() {
        return keyCloakClientId;
    }

    public String getKeyCloakSecret() {
        return keyCloakSecret;
    }

    public String getKeyCloakRealm() {
        return keyCloakRealm;
    }

    public String getKeyCloakBaseUri() {
        return keyCloakBaseUri;
    }
}
