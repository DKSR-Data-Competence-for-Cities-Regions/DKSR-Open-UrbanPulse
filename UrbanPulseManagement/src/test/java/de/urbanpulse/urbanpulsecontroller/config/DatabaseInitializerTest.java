package de.urbanpulse.urbanpulsecontroller.config;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.BackchannelSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ejb.EJBContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseInitializerTest {

    @InjectMocks
    private DatabaseInitializer dut = spy(DatabaseInitializer.class);

    @Mock
    private UserManagementDAO mockUserManagementDAO;

    @Mock
    private PersistenceV3SetupDAO mockPersistenceV3SetupDAO;

    @Mock
    private BackchannelSetupDAO mockBackchannelSetupDAO;

    @Mock
    private InboundSetupDAO mockInboundSetupDAO;

    @Mock
    private EJBContext mockContext;

    @Mock
    private UserTransaction mockUserTransaction;

    @Mock
    private EntityManager mockEntityManager;

    @Mock
    private TypedQuery<RoleEntity> mockTypedQuery;

    @Mock
    private TypedQuery<PermissionEntity> mockPermissionTypedQuery;

    @Before
    public void setupMocks() throws Exception {
        when(mockContext.getUserTransaction()).thenReturn(mockUserTransaction);

        doNothing().when(dut).createInsecureDefaultAdmin();
        doNothing().when(dut).executeSql(anyString());
    }

    @Test
    public void whenConfigIsNotEmpty() throws Exception {
        when(mockUserManagementDAO.getAdmins()).thenReturn(Arrays.asList(new UserTO()));
        when(mockInboundSetupDAO.hasConfig()).thenReturn(true);
        when(mockPersistenceV3SetupDAO.hasConfig()).thenReturn(true);
        when(mockBackchannelSetupDAO.hasConfig()).thenReturn(true);
        when(mockEntityManager.createNamedQuery("getRoleByName", RoleEntity.class)).thenReturn(mockTypedQuery);
        when(mockEntityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        when(mockPermissionTypedQuery.setParameter(anyString(),anyString())).thenReturn(mockPermissionTypedQuery);
        List<RoleEntity> list = new LinkedList<>();
        list.add(new RoleEntity());
        when(mockTypedQuery.getResultList()).thenReturn(list);

        List<PermissionEntity> permissionList = new LinkedList<>();
        permissionList.add(new PermissionEntity());
        when(mockPermissionTypedQuery.getResultList()).thenReturn(permissionList);

        dut.initializeDb();
        verify(mockEntityManager, never()).persist(any(RoleEntity.class));
        verify(dut, never()).createInsecureDefaultAdmin();
        verify(dut, never()).executeSql(anyString());
    }

    @Test
    public void whenConfigIsEmpty() throws Exception {
        when(mockUserManagementDAO.getAdmins()).thenReturn(new ArrayList<>());
        when(mockInboundSetupDAO.hasConfig()).thenReturn(false);
        when(mockPersistenceV3SetupDAO.hasConfig()).thenReturn(false);
        when(mockBackchannelSetupDAO.hasConfig()).thenReturn(false);
        when(mockEntityManager.createNamedQuery("getRoleByName", RoleEntity.class)).thenReturn(mockTypedQuery);
        when(mockEntityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        when(mockPermissionTypedQuery.setParameter(anyString(),anyString())).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.getResultList()).thenReturn(new LinkedList<>());
        when(mockPermissionTypedQuery.getResultList()).thenReturn(new LinkedList<>());

        dut.initializeDb();
        verify(mockEntityManager, times(6)).persist(any(RoleEntity.class));
        verify(dut).createInsecureDefaultAdmin();
        verify(dut).executeSql(endsWith("persistence.sql"));
        verify(dut).executeSql(endsWith("inbound.sql"));
        verify(dut).executeSql(endsWith("backchannel.sql"));
    }

    @Test
    public void createsPermissionsIfTheyNotExists(){
        when(mockUserManagementDAO.getAdmins()).thenReturn(new ArrayList<>());
        when(mockInboundSetupDAO.hasConfig()).thenReturn(false);
        when(mockPersistenceV3SetupDAO.hasConfig()).thenReturn(false);
        when(mockBackchannelSetupDAO.hasConfig()).thenReturn(false);
        when(mockEntityManager.createNamedQuery("getRoleByName", RoleEntity.class)).thenReturn(mockTypedQuery);
        when(mockEntityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        when(mockPermissionTypedQuery.setParameter(anyString(),anyString())).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.getResultList()).thenReturn(new LinkedList<>());
        when(mockPermissionTypedQuery.getResultList()).thenReturn(new LinkedList<>());

        dut.initializeDb();
        verify(mockEntityManager, times(4)).persist(any(PermissionEntity.class));
    }


    @Test
    public void doesNotCreatePermissionsIfTheyAlreadyExist(){
        when(mockUserManagementDAO.getAdmins()).thenReturn(new ArrayList<>());
        when(mockInboundSetupDAO.hasConfig()).thenReturn(false);
        when(mockPersistenceV3SetupDAO.hasConfig()).thenReturn(false);
        when(mockBackchannelSetupDAO.hasConfig()).thenReturn(false);
        when(mockEntityManager.createNamedQuery("getRoleByName", RoleEntity.class)).thenReturn(mockTypedQuery);
        when(mockEntityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)).thenReturn(mockPermissionTypedQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        when(mockPermissionTypedQuery.setParameter(anyString(),anyString())).thenReturn(mockPermissionTypedQuery);
        LinkedList<PermissionEntity> existingPermissions = new LinkedList<>();
        existingPermissions.add(new PermissionEntity());
        when(mockTypedQuery.getResultList()).thenReturn(new LinkedList<>());
        when(mockPermissionTypedQuery.getResultList()).thenReturn(existingPermissions);

        dut.initializeDb();
        verify(mockEntityManager,never()).persist(any(PermissionEntity.class));
    }

    @Test
    public void assignRolesToExistingPermissions(){
        when(mockUserManagementDAO.getAdmins()).thenReturn(new ArrayList<>());
        when(mockInboundSetupDAO.hasConfig()).thenReturn(false);
        when(mockPersistenceV3SetupDAO.hasConfig()).thenReturn(false);
        when(mockBackchannelSetupDAO.hasConfig()).thenReturn(false);

        when(mockEntityManager.createNamedQuery("getRoleByName", RoleEntity.class)).thenReturn(mockTypedQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        LinkedList<RoleEntity> existingRoles = new LinkedList<>();
        existingRoles.add(new RoleEntity());
        when(mockTypedQuery.getResultList()).thenReturn(existingRoles);

        when(mockEntityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)).thenReturn(mockPermissionTypedQuery);
        when(mockPermissionTypedQuery.setParameter(anyString(),any())).thenReturn(mockPermissionTypedQuery);
        LinkedList<PermissionEntity> existingPermissions = new LinkedList<>();
        existingPermissions.add(new PermissionEntity());
        when(mockPermissionTypedQuery.getResultList()).thenReturn(existingPermissions);

        dut.initializeDb();
        verify(mockEntityManager,never()).persist(any(PermissionEntity.class));
        verify(mockEntityManager, times(3)).merge(any());
        verify(mockEntityManager, times(3)).flush();
    }
}
