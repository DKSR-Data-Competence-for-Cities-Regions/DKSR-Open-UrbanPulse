package de.urbanpulse.auth.upsecurityrealm.shiro;

import de.urbanpulse.dist.jee.upsecurityrealm.UPPasswordService;
import javax.sql.DataSource;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.jdbc.JdbcRealm;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SqlRealm extends JdbcRealm {

    /**
     * get passwordhash of user
     */
    private static final String UP_AUTHENTICATION_QUERY = "SELECT passwordhash FROM up_users WHERE name = ?";

    /**
     * get rolenames for user
     */
    private static final String UP_USER_ROLES_QUERY = "SELECT r.name FROM up_roles as r INNER JOIN up_user_roles as ur ON r.id = ur.role_id INNER JOIN up_users as u ON u.id = ur.user_id WHERE u.name = ?";

    /**
     * get permissionnames from rolename
     */
    private static final String UP_ROLE_PERMISSIONS_QUERY = "SELECT p.name FROM up_permissions as p INNER JOIN up_role_permissions as rp ON p.id = rp.permission_id INNER JOIN up_roles as r ON r.id = rp.role_id WHERE r.name = ?";

    private String realmName;

    public SqlRealm(DataSource dataSource, boolean useCredentialsMatcher, String realmName) {
        this(useCredentialsMatcher,realmName);
        setDataSource(dataSource);
    }

    public SqlRealm(boolean useCredentialsMatcher, String realmName) {
        setAuthenticationQuery(UP_AUTHENTICATION_QUERY);
        setUserRolesQuery(UP_USER_ROLES_QUERY);
        setPermissionsQuery(UP_ROLE_PERMISSIONS_QUERY);
        this.setPermissionsLookupEnabled(true);
        //we dont need the credentials matcher when this realm is used for bearer token authZ/N
        if (useCredentialsMatcher) {
            PasswordMatcher credentialsMatcher = new PasswordMatcher();
            credentialsMatcher.setPasswordService(new UPPasswordService());
            this.setCredentialsMatcher(credentialsMatcher);
        }
        this.realmName = realmName;

    }

    @Override
    public String getName(){
        return realmName;
    }
}
