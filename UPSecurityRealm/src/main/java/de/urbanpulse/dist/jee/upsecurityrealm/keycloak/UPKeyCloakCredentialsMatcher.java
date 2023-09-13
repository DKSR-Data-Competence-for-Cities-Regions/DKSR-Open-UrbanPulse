package de.urbanpulse.dist.jee.upsecurityrealm.keycloak;

import de.urbanpulse.dist.jee.upsecurityrealm.*;
import org.apache.shiro.authc.credential.PasswordMatcher;

public class UPKeyCloakCredentialsMatcher extends PasswordMatcher {

    public UPKeyCloakCredentialsMatcher() {
        this.setPasswordService(new UPPasswordService());
    }
    
}
