package de.urbanpulse.dist.jee.upsecurityrealm.oidc;
import org.apache.shiro.authc.AuthenticationToken;

public class OidcToken implements AuthenticationToken {

    private String accessToken;

    public OidcToken(String accessToken){
        this.accessToken = accessToken;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return accessToken;
    }
}

