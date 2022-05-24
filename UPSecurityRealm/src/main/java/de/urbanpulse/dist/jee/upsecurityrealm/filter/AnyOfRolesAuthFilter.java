package de.urbanpulse.dist.jee.upsecurityrealm.filter;

import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AnyOfRolesAuthFilter extends RolesAuthorizationFilter {

    @Override
    public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws IOException {
        final Subject subject = getSubject(request, response);
        final String[] requestedRoles = (String[]) mappedValue;

        //accept any roles when no roles are specified
        if (requestedRoles == null || requestedRoles.length == 0) {
            return true;
        }

        for (String roleName : requestedRoles) {
            if (subject.hasRole(roleName)) {
                return true;
            }
        }

        return false;
    }
}
