package de.urbanpulse.auth.upsecurityrealm.shiro;

import java.util.Collection;
import java.util.List;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UnsecurityManager implements org.apache.shiro.mgt.SecurityManager {

    @Override
    public Subject login(Subject subject, AuthenticationToken authenticationToken) {
        return new UnsecureSubject();
    }

    @Override
    public void logout(Subject subject) {
        // Nothing to do. We weren't logged in in the first place.
    }

    @Override
    public Subject createSubject(SubjectContext context) {
        return new UnsecureSubject();
    }

    @Override
    public AuthenticationInfo authenticate(AuthenticationToken authenticationToken) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        return true;
    }

    @Override
    public boolean isPermitted(PrincipalCollection subjectPrincipal, Permission permission) {
        return true;
    }

    @Override
    public boolean[] isPermitted(PrincipalCollection subjectPrincipal, String... permissions) {
        boolean[] result = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean[] isPermitted(PrincipalCollection subjectPrincipal, List<Permission> permissions) {
        boolean[] result = new boolean[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean isPermittedAll(PrincipalCollection subjectPrincipal, String... permissions) {
        return true;
    }

    @Override
    public boolean isPermittedAll(PrincipalCollection subjectPrincipal, Collection<Permission> permissions) {
        return true;
    }

    @Override
    public void checkPermission(PrincipalCollection subjectPrincipal, String permission) {

    }

    @Override
    public void checkPermission(PrincipalCollection subjectPrincipal, Permission permission) {

    }

    @Override
    public void checkPermissions(PrincipalCollection subjectPrincipal, String... permissions) {

    }

    @Override
    public void checkPermissions(PrincipalCollection subjectPrincipal, Collection<Permission> permissions) {

    }

    @Override
    public boolean hasRole(PrincipalCollection subjectPrincipal, String roleIdentifier) {
        return true;
    }

    @Override
    public boolean[] hasRoles(PrincipalCollection subjectPrincipal, List<String> roleIdentifiers) {
        boolean[] result = new boolean[roleIdentifiers.size()];
        for (int i = 0; i < roleIdentifiers.size(); i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean hasAllRoles(PrincipalCollection subjectPrincipal, Collection<String> roleIdentifiers) {
        return true;
    }

    @Override
    public void checkRole(PrincipalCollection subjectPrincipal, String roleIdentifier) {

    }

    @Override
    public void checkRoles(PrincipalCollection subjectPrincipal, Collection<String> roleIdentifiers) {

    }

    @Override
    public void checkRoles(PrincipalCollection subjectPrincipal, String... roleIdentifiers) {

    }

    @Override
    public Session start(SessionContext context) {
        return new SimpleSession();
    }

    @Override
    public Session getSession(SessionKey key) {
        return new SimpleSession();
    }

}
