package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles;
import de.urbanpulse.urbanpulsemanagement.restfacades.RolesRestFacade;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.helper.ShiroSubjectHelper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.PersistenceV3Wrapper;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.junit.After;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class RolesRestServiceTest {

    private static final URI ABSOLUTE_PATH = URI.create("https://foo.bar/some/absolute/path/with/https/");
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/users/42");

    private static final String USER_ID = "42";
    private static final String USER_NAME = "Fritz";

    private static final String OTHER_USER_NAME = "Remy";

    private static final String USER_PASSWD = "superSecret";
    private static final RoleTO ROLE_APP_USER = new RoleTO(UUID.randomUUID().toString(), UPDefaultRoles.APP_USER);
    private static final RoleTO ROLE_CONNECTOR_MANAGER = new RoleTO(UUID.randomUUID().toString(), UPDefaultRoles.CONNECTOR_MANAGER);
    private static final RoleTO ROLE_ADMIN = new RoleTO(UUID.randomUUID().toString(), UPDefaultRoles.ADMIN);
    private static final String NON_EXISTING_ROLE_ID = UUID.randomUUID().toString();

    private static final PermissionTO PERMISSION_NONADMIN = new PermissionTO("Nose picking");
    private static final PermissionTO PERMISSION_ADMIN = new PermissionTO("Delete everything");

    @Mock(name = "dao")
    private RoleManagementDAO roleDaoMock;

    @Mock
    private PermissionManagementDAO permissionDao;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock(name = "context")
    protected UriInfo contextMock;

    @Mock
    private RolesRestFacade rolesRestFacadeMock;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    RoleTO roleToMock;

    @InjectMocks
    private RolesRestService service;

    @Mock
    private Principal mockUserPrincipal;

    @Mock
    private PersistenceV3Wrapper mockPersistenceV3Wrapper;

    @Mock
    private Subject mockSubject;

    @Mock
    private ShiroSubjectHelper mockShiroSubjectHelper;

    @Mock
    private UserTO nonAdmin;

    @Mock
    private UserTO admin;

    @Before
    public void setUp() {
        given(roleDaoMock.getById(ROLE_ADMIN.getId())).willReturn(ROLE_ADMIN);
        given(roleDaoMock.getById(NON_EXISTING_ROLE_ID)).willReturn(null);

        given(roleDaoMock.createRole(any())).willReturn(roleToMock);

        given(permissionDao.getById(PERMISSION_ADMIN.getId())).willReturn(PERMISSION_ADMIN);
        given(permissionDao.getById(PERMISSION_NONADMIN.getId())).willReturn(PERMISSION_NONADMIN);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetRoles_returnsExpected() throws Exception {
        List<RoleTO> roles = new ArrayList<>();
        roles.add(roleToMock);

        given(roleDaoMock.getAll()).willReturn(roles);

        Response response = service.getRoles();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetRoles_returnsEmpty() throws Exception {
        given(roleDaoMock.getAll()).willReturn(null);

        Response response = service.getRoles();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetRole_returnExpected() throws Exception {
        Response response = service.getRole(ROLE_ADMIN.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetRole_roleDoesNotExist() throws Exception {
        Response response = service.getRole(NON_EXISTING_ROLE_ID);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateRole_returnsExpected() throws Exception {
        given(contextMock.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);
        given(roleDaoMock.createRole(any(RoleTO.class))).willReturn(roleToMock);
        given(roleToMock.getId()).willReturn("42");

        RoleTO role = new RoleTO();
        role.setName("Rollator");
        role.setPermissions(Arrays.asList(PERMISSION_NONADMIN));

        Response response = service.createRole(role, contextMock, rolesRestFacadeMock);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateRole_returnsUnprocEntityForUnsupportedPermissions() throws Exception {

        given(permissionDao.getById("bogusPermission")).willReturn(null);

        RoleTO role = new RoleTO();
        role.setName("Hamlet");
        role.setPermissions(Arrays.asList(new PermissionTO("bogusPermission", "bogusPermission")));

        Response response = service.createRole(role, contextMock, rolesRestFacadeMock);

        assertEquals(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void testDeleteRole_returnsExpected() throws Exception {
        Response response = service.deleteRole(ROLE_APP_USER.getId());

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(roleDaoMock).deleteById(ROLE_APP_USER.getId());
    }

    @Test
    public void addPermission_returnsBadRequestForNotValidJsonBody() {
        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        ScopesWithOperations scopesWithOperations = new ScopesWithOperations();

        Response response = service.addPermission(user.getId(), "sensorId", scopesWithOperations);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void addPermission_updateRolesPermissionSuccessfully_ifThePermissionAlreadyExistsInTheDatabase() {
        PermissionTO permissionTO = new PermissionTO("sensor:sid1:historicdata:read");
        List<PermissionTO> permissions = new ArrayList<>();
        permissions.add(permissionTO);

        given(roleDaoMock.getById(Mockito.anyString())).willReturn(roleToMock);
        given(roleToMock.getPermissions()).willReturn(new ArrayList<>());
        given(permissionDao.getFilteredBy(anyString(), any())).willReturn(permissions);

        RoleTO role = new RoleTO();
        role.setId("id1");
        role.setName("role1");
        role.setPermissions(permissions);

        ScopesWithOperations scopesWithOperations = new ScopesWithOperations();
        List<String> operations = new ArrayList<>();
        operations.add("read");
        scopesWithOperations.setOperation(operations);
        List<String> scopes = new ArrayList<>();
        scopes.add("historicdata");
        scopesWithOperations.setScope(scopes);

        Response response = service.addPermission(role.getId(), "sid1", scopesWithOperations);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void addPermission_whenPermissionDoesNotExists_thenReturnBadRequest() {
        RoleTO role = new RoleTO();
        role.setId("id1");
        role.setName("role1");
        role.setPermissions(Collections.EMPTY_LIST);

        ScopesWithOperations scopesWithOperations = new ScopesWithOperations();
        List<String> operations = new ArrayList<>();
        operations.add("read");
        scopesWithOperations.setOperation(operations);
        List<String> scopes = new ArrayList<>();
        scopes.add("historicdata");
        scopesWithOperations.setScope(scopes);

        Response response = service.addPermission(role.getId(), "sid1", scopesWithOperations);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPermission_successfullyGetPermissions() {
        RoleTO role = new RoleTO();
        role.setId("id1");
        role.setName("role1");
        role.setPermissions(Collections.EMPTY_LIST);

        Response response = service.getPermission(role.getId(), "sid1");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePermission_successfullyDeletePermission() {
        PermissionTO permissionTO = new PermissionTO("permissionId", "sensor:sid1:historicdata:read");
        List<PermissionTO> permissions = new ArrayList<>();
        permissions.add(permissionTO);

        RoleTO role = new RoleTO();
        role.setId("id1");
        role.setName("role1");
        role.setPermissions(permissions);

        given(roleDaoMock.getById(Mockito.anyString())).willReturn(role);

        Response response = service.deletePermission("id1", "permissionId");

        verify(roleDaoMock).updateRole(role);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePermission_returnsBadRequestBecauseThePermissionIsNotAssignedToTheRole() {
        RoleTO role = new RoleTO();
        role.setId("id1");
        role.setName("role1");
        role.setPermissions(Collections.EMPTY_LIST);

        given(roleDaoMock.getById(Mockito.anyString())).willReturn(role);

        Response response = service.deletePermission("id1", "permissionId");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
