package de.urbanpulse.dist.outbound.server.auth;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class OutboundSqlRealmTest {

    private OutboundSqlRealm outboundSqlRealm;

    @Before
    public void init() {
        BasicDataSource basicDataSource = new BasicDataSource();
        outboundSqlRealm = new OutboundSqlRealm(basicDataSource, true, "TestSqlRealm");
    }

    @Test
    public void test_getPermissions_whenTheUserHasNoPermission() throws SQLException {
        Connection mockConnection = Mockito.mock(Connection.class);
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(mockConnection.prepareStatement(Mockito.any())).thenReturn(mockPreparedStatement);

        Mockito.when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.next()).thenReturn(false);

        Collection<String> dummyRolenames = new ArrayList<>();
        Set<String> permissions = outboundSqlRealm.getPermissions(mockConnection, "admin", dummyRolenames);

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void test_getPermission_whenTheUserHasPermission() throws SQLException {
        Connection mockConnection = Mockito.mock(Connection.class);
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(mockConnection.prepareStatement(Mockito.any())).thenReturn(mockPreparedStatement);
        Mockito.when(mockResultSet.getString(1)).thenReturn("sensor:*:livedata:*");

        Mockito.when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(false);

        Collection<String> dummyRolenames = new ArrayList<>();
        Set<String> permissions = outboundSqlRealm.getPermissions(mockConnection, "admin", dummyRolenames);

        assertFalse(permissions.isEmpty());
        assertTrue(permissions.contains("sensor:*:livedata:*"));
    }

    @Test
    public void test_getPermission_whenTheUserHasNullPermission() throws SQLException {
        Connection mockConnection = Mockito.mock(Connection.class);
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(mockConnection.prepareStatement(Mockito.any())).thenReturn(mockPreparedStatement);
        Mockito.when(mockResultSet.getString(1)).thenReturn(null);

        Mockito.when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(false);

        Collection<String> dummyRolenames = new ArrayList<>();
        Set<String> permissions = outboundSqlRealm.getPermissions(mockConnection, "admin", dummyRolenames);

        assertTrue(permissions.isEmpty());
    }
}
