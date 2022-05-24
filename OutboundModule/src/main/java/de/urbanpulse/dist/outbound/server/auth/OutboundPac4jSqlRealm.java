package de.urbanpulse.dist.outbound.server.auth;


import org.apache.commons.dbcp.BasicDataSource;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.JdbcUtils;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class OutboundPac4jSqlRealm extends OutboundSqlRealm {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundPac4jSqlRealm.class);
    private static final String REALM_NAME = "OutboundPac4jSqlRealm";

    private UserInfoOidcAuthenticator userInfoOidcAuthenticator;

    private String urbanPulseUrl;

    public OutboundPac4jSqlRealm(BasicDataSource dataSource, boolean useCredentialsMatch,
                                 UserInfoOidcAuthenticator userInfoOidcAuthenticator, String urbanPulseUrl) {
        super(dataSource, useCredentialsMatch, REALM_NAME);

        this.userInfoOidcAuthenticator = userInfoOidcAuthenticator;
        this.urbanPulseUrl = urbanPulseUrl;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo simpleAuthorizationInfo = null;
        Object[] principalCollection = principals.fromRealm(REALM_NAME).toArray();

        if (principalCollection.length == 2) {
            Connection connection = null;

            try {
                connection = dataSource.getConnection();

                String preferredUserName = (String) principalCollection[1];

                TokenCredentials tokenCredentials = (TokenCredentials) principalCollection[0];
                Set<String> permissions = new HashSet<>();

                simpleAuthorizationInfo.setStringPermissions(permissions);
            } catch (SQLException throwable) {
                LOG.error("Can not authorize user!", throwable);
            } finally {
                JdbcUtils.closeConnection(connection);
            }

        } else {
            LOG.error("Principal collection is not full! Missing information authorization will be invalidated...");
        }


        return simpleAuthorizationInfo;
    }




}
