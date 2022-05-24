package de.urbanpulse.urbanpulsemanagement.services.helper;

import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import javax.enterprise.context.Dependent;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Dependent
public class ShiroSubjectHelper {

    public Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    public String getSubjectId() {
        Subject currentUser = getSubject();
        LoginToken token = (LoginToken)currentUser.getPrincipals().iterator().next();
        return token.getSubjectId();
    }

}
