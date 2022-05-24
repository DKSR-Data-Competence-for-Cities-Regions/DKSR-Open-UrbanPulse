package de.urbanpulse.auth.upsecurityrealm.shiro;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.shiro.impl.ShiroUser;
import java.util.Arrays;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UpShiroUser extends ShiroUser {

    private final String rolesPrefix;
    private final Vertx vertx;
    private final Subject subject;


    public UpShiroUser() {
        this.rolesPrefix = null;
        this.vertx = null;
        this.subject = null;
    }

    UpShiroUser(Vertx vertx, SecurityManager securityManager, Subject subject, String rolePrefix, String rolesPrefix) {
        super(vertx, securityManager, subject, rolePrefix);
        this.vertx = vertx;
        this.rolesPrefix = rolesPrefix;
        this.subject = subject;
    }

    @Override
    protected void doIsPermitted(String permissionOrRole, Handler<AsyncResult<Boolean>> resultHandler) {
        vertx.executeBlocking(fut -> {
            if (permissionOrRole.startsWith(rolesPrefix)) {
                boolean[] rolesResult = subject.hasRoles(Arrays.asList(permissionOrRole.substring(rolesPrefix.length()).split(",")));
                boolean hasAnyRole = any(rolesResult);
                fut.complete(hasAnyRole);
            } else {
                super.doIsPermitted(permissionOrRole, resultHandler);
            }
        }, resultHandler);
    }

    /**
     * @param booleans
     * @return true if any of the booleans is true, else false
     */
    private boolean any(boolean[] booleans) {
        for (int i = 0; i < booleans.length; i++) {
            if (booleans[i]) {
                return true;
            }
        }
        return false;
    }
}
