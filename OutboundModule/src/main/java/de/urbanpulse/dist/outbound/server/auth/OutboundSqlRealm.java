package de.urbanpulse.dist.outbound.server.auth;

import de.urbanpulse.auth.upsecurityrealm.shiro.SqlRealm;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundSqlRealm extends SqlRealm {

    private static final String DEFAULT_USER_PERMISSION_QUERY = "SELECT p.name FROM up_permissions as p INNER JOIN up_user_permissions as up ON p.id = up.permission_id INNER JOIN up_users as u ON u.id = up.user_id WHERE u.name = ?";
    private static final Logger LOG = LoggerFactory.getLogger(OutboundSqlRealm.class);

    private String userPermissionQuery = "";

    public OutboundSqlRealm(BasicDataSource dataSource, boolean userCredentialsMatcher, String realmName) {
        super(dataSource,userCredentialsMatcher,realmName);
        setUserPermissionQuery(DEFAULT_USER_PERMISSION_QUERY);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Object[] principalCollection = principals.fromRealm(getName()).toArray();

        //The problem is that the JdbcRealm (the SqlRealm extends it) does not check if the
        //principals are meant for him or not. So if a request gets not permitted for a user with a realm from
        //org.apache.shiro.authz.ModularRealmAuthorizer.isPermitted the next realm in
        //the List will be called (the JdbcRealm) causing a casting exception. So we overwrite the method
        //to check if this is for the correct realm or not, if so we just call the method in parent
        //class otherwise return null (we don't know how to handle this principals) resulting in a 403 response.
         if (principalCollection.length <= 0) {
            return null;
        } else {
            return super.doGetAuthorizationInfo(principals);
        }
    }

    @Override
    protected Set<String> getPermissions(Connection conn, String username, Collection<String> roleNames) throws SQLException {
        Set<String> permissions = super.getPermissions(conn, username, roleNames);
        permissions.addAll(getUserPermissions(conn, username));
        return permissions;
    }

    protected void setUserPermissionQuery(String query) {
        userPermissionQuery = query;
    }

    protected Set<String> getUserPermissions(Connection conn, String username) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Set<String> permissions = new LinkedHashSet<>();
        try {
            ps = conn.prepareStatement(userPermissionQuery);
            ps.setString(1, username);

            // Execute query
            rs = ps.executeQuery();

            // Loop over results and add each returned permission to a set
            while (rs.next()) {

                String permissionName = rs.getString(1);

                // Add the permission to the list of names if it isn't null
                if (permissionName != null) {
                    permissions.add(permissionName);
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Null permission name found while retrieving role names for user: {}", username);
                    }
                }
            }
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(ps);
        }
        return permissions;
    }
}
