package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import javax.servlet.ServletRequest;
import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HmacToken extends UsernamePasswordToken {

    private ServletRequest request;
    private String accessKeyId;
    private String signature;
    private UPAuthMode authMode;

    /**
     *
     * @param accessKeyId Base64 encoded id/name
     * @param signature HMAC265 signature
     * @param request the ServletRequest object
     * @param authMode UP or UPCONNECTOR
     */
    public HmacToken(String accessKeyId, String signature, ServletRequest request, UPAuthMode authMode) {
        super();

        setUsername(accessKeyId);
        setRememberMe(false);
        if (request != null) {
            setHost(request.getRemoteHost());
        }
        this.authMode = authMode;
        this.request = request;
        this.accessKeyId = accessKeyId;
        this.signature = signature;
    }


    @Override
    public Object getCredentials() {
        return getSignature();
    }

    public ServletRequest getRequest() {
        return this.request;
    }

    public void setRequest(ServletRequest request) {
        this.request = request;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean isRememberMe() {
        return false;
    }

    public UPAuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(UPAuthMode authMode) {
        this.authMode = authMode;
    }


}
