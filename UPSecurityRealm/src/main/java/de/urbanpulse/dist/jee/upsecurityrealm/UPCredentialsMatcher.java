package de.urbanpulse.dist.jee.upsecurityrealm;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPCredentialsMatcher extends SimpleCredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        Object tokenCredentials = this.getCredentials(token);
        Object accountCredentials = this.getCredentials(info);

        if (tokenCredentials instanceof char[]) {
            String password = String.valueOf((char[]) tokenCredentials);
            return UPPasswordService.doCredentialsMatch(password, (String) accountCredentials);
        }
        throw new IllegalArgumentException("This matcher only supports char[] credentials!");
    }
}
