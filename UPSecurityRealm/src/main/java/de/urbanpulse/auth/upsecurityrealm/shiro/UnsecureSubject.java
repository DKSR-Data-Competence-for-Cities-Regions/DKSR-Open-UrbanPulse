package de.urbanpulse.auth.upsecurityrealm.shiro;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UnsecureSubject implements Subject {

    private static final String UNSUPPORTED_MESSAGE = "Not supported yet.";

    @Override
    public Object getPrincipal() {
        return "anony";
    }

    @Override
    public PrincipalCollection getPrincipals() {
        return new SimplePrincipalCollection();
    }

    @Override
    public boolean isPermitted(String permission) {
        return true;
    }

    @Override
    public boolean isPermitted(Permission permission) {
        return true;
    }

    @Override
    public boolean[] isPermitted(String... permissions) {
        boolean[] result = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean[] isPermitted(List<Permission> permissions) {
        boolean[] result = new boolean[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean isPermittedAll(String... permissions) {
        return true;
    }

    @Override
    public boolean isPermittedAll(Collection<Permission> permissions) {
        return true;
    }

    @Override
    public void checkPermission(String permission) {

    }

    @Override
    public void checkPermission(Permission permission) {

    }

    @Override
    public void checkPermissions(String... permissions) {

    }

    @Override
    public void checkPermissions(Collection<Permission> permissions) {

    }

    @Override
    public boolean hasRole(String roleIdentifier) {
        return true;
    }

    @Override
    public boolean[] hasRoles(List<String> roleIdentifiers) {
        boolean[] result = new boolean[roleIdentifiers.size()];
        for (int i = 0; i < roleIdentifiers.size(); i++) {
            result[i] = true;
        }
        return result;
    }

    @Override
    public boolean hasAllRoles(Collection<String> roleIdentifiers) {
        return true;
    }

    @Override
    public void checkRole(String roleIdentifier) {

    }

    @Override
    public void checkRoles(Collection<String> roleIdentifiers) {

    }

    @Override
    public void checkRoles(String... roleIdentifiers) {

    }

    @Override
    public void login(AuthenticationToken token) {

    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public boolean isRemembered() {
        return true;
    }

    @Override
    public Session getSession() {
        return new SimpleSession();
    }

    @Override
    public Session getSession(boolean create) {
        return new SimpleSession();
    }

    @Override
    public void logout() {

    }

    @Override
    public <V> V execute(Callable<V> callable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void execute(Runnable runnable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public <V> Callable<V> associateWith(Callable<V> callable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Runnable associateWith(Runnable runnable) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void runAs(PrincipalCollection principals) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public boolean isRunAs() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }


    @Override
    public PrincipalCollection getPreviousPrincipals() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public PrincipalCollection releaseRunAs() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

}
