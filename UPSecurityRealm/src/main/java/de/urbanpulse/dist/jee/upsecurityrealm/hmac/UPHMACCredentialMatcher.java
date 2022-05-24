/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.web.util.WebUtils;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPHMACCredentialMatcher extends SimpleCredentialsMatcher {

    private static final Logger LOG = Logger.getLogger(UPHMACCredentialMatcher.class.getName());

    /**
     * Example Request
     * Authorization: UPCONNECTOR YWRtaW4=:lv+n2NIMJUFYuxA8JW5+qDSX0zen+SI9PoTVVsuE/10=
     * UrbanPulse-Timestamp: 2017-03-31T16:31:54.825+0000
     *
     * @param token the authentication (HMAC) token
     * @param info the authentication info
     * @return if credentials match or not
     */
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        HmacToken hmacToken = (HmacToken) token;
        HttpServletRequest httpRequest = WebUtils.toHttp(hmacToken.getRequest());
        String method = httpRequest.getMethod();

        String stringToHash;

        switch (method.toLowerCase()) {
            case "post":
            case "put": {
                stringToHash = createBodySignature(httpRequest);
            }
            break;
            default: {
                stringToHash = createPathSignature(httpRequest);
            }
        }

        String hashedString;
        try {
            hashedString = Hasher.createHmac256((String) info.getCredentials(), stringToHash);
            return hmacToken.getSignature().equals(hashedString);
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException ex) {
            Logger.getLogger(UPHMACCredentialMatcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    private String createBodySignature(HttpServletRequest httpRequest) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(httpRequest.getInputStream(), Charset.forName("utf-8")));
            br.lines().forEachOrdered(l -> sb.append(l));
        } catch (Exception ex) {
            Logger.getLogger(UPHMACCredentialMatcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        return getUPTimestamp(httpRequest) + sb.toString();
    }

    private String createPathSignature(HttpServletRequest httpRequest) {
        return getUPTimestamp(httpRequest) + httpRequest.getRequestURI();
    }

    private String getUPTimestamp(HttpServletRequest httpRequest) {
        return httpRequest.getHeader(UPHmacAuthenticationFilter.UP_TIMESTAMP_HEADER);
    }

}
