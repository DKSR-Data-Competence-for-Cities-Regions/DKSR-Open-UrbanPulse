package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import java.util.Map;

/**
 * This token is used to forward the UPAuthMode to form the authentication step to the authorization
 * @see de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHMACSecurityRealm
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class LoginToken {

    private UPAuthMode authmode;
    private String subjectId;
    private String realmName;
    private Map<String, Object> payload;

    public LoginToken(UPAuthMode authmode, String subjectId) {
        this.authmode = authmode;
        this.subjectId = subjectId;
    }

    public UPAuthMode getAuthmode() {
        return authmode;
    }

    public void setAuthmode(UPAuthMode authmode) {
        this.authmode = authmode;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "LoginToken{" + "authmode=" + authmode + ", subjectId=" + subjectId + ", realmName=" + realmName + '}';
    }



}
