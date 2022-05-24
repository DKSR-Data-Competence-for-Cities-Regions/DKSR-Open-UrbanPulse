package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles;
import de.urbanpulse.urbanpulsemanagement.restfacades.UsersRestFacade;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.helper.ShiroSubjectHelper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.PersistenceV3Wrapper;
import de.urbanpulse.urbanpulsemanagement.util.PasswordPolicy;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UsersRestServiceTest {

    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/users/42");

    private static final String USER_ID = "42";
    private static final String OTHER_USER_ID = "52";
    private static final String USER_NAME = "Fritz";

    private static final String OTHER_USER_NAME = "Remy";

    private static final String USER_PASSWD = "superSecret";

    private static final PermissionTO PERMISSION_NONADMIN = new PermissionTO("Nose picking");
    private static final PermissionTO PERMISSION_ADMIN = new PermissionTO("Delete everything");

    @Mock
    private PasswordPolicy mockPasswordPolicy;

    @Mock(name = "dao")
    private UserManagementDAO userDaoMock;

    @Mock
    private PermissionManagementDAO permissionDao;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock(name = "context")
    protected UriInfo contextMock;

    @Mock
    UsersRestFacade usersRestFacadeMock;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    UserTO userToMock;

    @InjectMocks
    private UsersRestService service;

    @Mock
    private Principal mockUserPrincipal;

    @Mock
    private PersistenceV3Wrapper mockPersistenceV3Wrapper;

    @Mock
    private Subject mockSubject;

    @Mock
    private ShiroSubjectHelper mockShiroSubjectHelper;

    @Mock
    private RoleManagementDAO roleDao;

    @Mock
    private UserTO admin;

    @Before
    public void setUp() {


        given(mockSecurityContext.getUserPrincipal()).willReturn(mockUserPrincipal);


        given(permissionDao.getById(PERMISSION_ADMIN.getId())).willReturn(PERMISSION_ADMIN);
        given(permissionDao.getById(PERMISSION_NONADMIN.getId())).willReturn(PERMISSION_NONADMIN);

        given(mockShiroSubjectHelper.getSubject()).willReturn(mockSubject);
        given(mockShiroSubjectHelper.getSubjectId()).willReturn(USER_ID);

        SubjectThreadState subjectThreadState = new SubjectThreadState(mockSubject);
        subjectThreadState.bind();
    }

    @Test
    public void testGetUsers_returnsExpected() throws Exception {
        List<UserTO> users = new ArrayList<>();
        users.add(userToMock);

        given(userDaoMock.getAll()).willReturn(users);

        Response response = service.getUsers();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUsers_returnsEmpty() throws Exception {
        given(userDaoMock.getAll()).willReturn(null);

        Response response = service.getUsers();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUser_returnExpected() throws Exception {
        given(mockSubject.hasRole(anyString())).willReturn(Boolean.TRUE);
        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);

        Response response = service.getUser(USER_ID);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUser_userDoesNotExist() throws Exception {
        given(mockSubject.hasRole(anyString())).willReturn(Boolean.TRUE);
        given(userDaoMock.getById(USER_ID)).willReturn(null);

        Response response = service.getUser(USER_ID);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateUser_returnsExpected() throws Exception {
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(true);
        given(userToMock.getId()).willReturn("AnID");
        given(userDaoMock.createUser(any())).willReturn(userToMock);

        given(contextMock.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        UserTO user = new UserTO();
        user.setName("Fritz");
        user.setPassword("superSecret");
        user.setPermissions(Arrays.asList(PERMISSION_NONADMIN));

        Response response = service.createUser(user, contextMock, usersRestFacadeMock);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateUser_returnsUnprocEntityForUnsupportedRoles() throws Exception {
        UserTO user = new UserTO();
        user.setName("Fritz");
        user.setPassword("superSecret");
        user.setRoles(Arrays.asList(new RoleTO("bogusRole", "bogusRole")));

        Response response = service.createUser(user, contextMock, usersRestFacadeMock);

        assertEquals(AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
    }

    @Test
    public void testCreateUser_returnsBadRequestForUnacceptablePassword() throws Exception {
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(false);

        UserTO user = new UserTO();
        user.setName("Fritz");
        user.setPassword("superSecret");
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.createUser(user, contextMock, usersRestFacadeMock);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateUser_failsWithConflict() throws Exception {
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(true);
        given(userDaoMock.createUser(any())).willReturn(null);

        UserTO user = new UserTO();
        user.setName("Fritz");
        user.setPassword("superSecret");
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.createUser(user, contextMock, usersRestFacadeMock);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());

    }

    @Test
    public void testDeleteUser_returnsExpected() throws Exception {
        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);

        Response response = service.deleteUser(USER_ID);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(userDaoMock).deleteById(USER_ID);
    }

    @Test
    public void resetKey_doesNotAllowUserToChangeAnotherUserkey() throws Exception {
        given(mockSubject.getPrincipal()).willReturn(new LoginToken(UPAuthMode.BASIC, OTHER_USER_ID));

        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);
        Response response = service.resetKey(USER_ID, mockSecurityContext);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void resetKey_AllowUserToChangeOwnUserkey() throws Exception {
        given(mockSubject.getPrincipal()).willReturn(new LoginToken(UPAuthMode.BASIC, USER_ID));

        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);
        given(userDaoMock.update(userToMock, true)).willReturn(userToMock);

        Response response = service.resetKey(USER_ID, mockSecurityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void resetKey_DoesNotAllowTokenLoginForChangingKey() throws Exception {
        given(mockSubject.getPrincipal()).willReturn(new LoginToken(UPAuthMode.UP, USER_ID));

        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);

        Response response = service.resetKey(USER_ID, mockSecurityContext);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    public void resetKey_ChangesSecretKey() throws Exception {
        given(mockUserPrincipal.getName()).willReturn(USER_NAME);
        given(mockSecurityContext.isUserInRole(UPDefaultRoles.ADMIN)).willReturn(true);

        given(userDaoMock.getById(USER_ID)).willReturn(admin);
        given(admin.getName()).willReturn(USER_NAME);

        admin.setSecretKey("Key1");
        Response response = service.resetKey(USER_ID, mockSecurityContext);

        verify(userDaoMock).update(admin);

        assertNotEquals("Key1", admin.getSecretKey());
    }

    @Test
    public void updateUser_allowsNonAdminToChangeOwnPassword() throws Exception {
        given(mockUserPrincipal.getName()).willReturn(USER_NAME);
        given(mockSecurityContext.isUserInRole(UPDefaultRoles.ADMIN)).willReturn(false);
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(true);

        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);
        given(userToMock.getName()).willReturn(USER_NAME);

        UserTO user = new UserTO();
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.updateUser(USER_ID, user, mockSecurityContext);

        verify(userDaoMock).update(user);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateUser_allowsAdminToChangeAnyPassword() throws Exception {
        given(mockUserPrincipal.getName()).willReturn(OTHER_USER_NAME);
        given(mockSecurityContext.isUserInRole(UPDefaultRoles.ADMIN)).willReturn(true);
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(true);

        given(userDaoMock.getById(USER_ID)).willReturn(admin);
        given(admin.getName()).willReturn(USER_NAME);

        UserTO user = new UserTO();
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.updateUser(USER_ID, user, mockSecurityContext);

        verify(userDaoMock).update(user);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateUser_returnsBadRequestForUnacceptablePassword() throws Exception {
        given(mockUserPrincipal.getName()).willReturn(USER_NAME);
        given(mockSecurityContext.isUserInRole(UPDefaultRoles.ADMIN)).willReturn(false);
        given(mockPasswordPolicy.isAcceptable(USER_PASSWD)).willReturn(false);

        given(userDaoMock.getById(USER_ID)).willReturn(userToMock);
        given(userToMock.getName()).willReturn(USER_NAME);

        UserTO user = new UserTO();
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.updateUser(USER_ID, user, mockSecurityContext);

        verify(userDaoMock, never()).update(any());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
    public void addPermission_updateUserPermissionSuccessfully_ifThePermissionAlreadyExistsInTheDatabase() {
        PermissionTO permissionTO = new PermissionTO("sensor:sid1:historicdata:read");
        List<PermissionTO> permissions = new ArrayList<>();
        permissions.add(permissionTO);

        given(userDaoMock.getById(Mockito.anyString())).willReturn(userToMock);
        given(userToMock.getPermissions()).willReturn(new ArrayList<>());
        given(permissionDao.getFilteredBy(anyString(), any())).willReturn(permissions);

        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        ScopesWithOperations scopesWithOperations = new ScopesWithOperations();
        List<String> operations = new ArrayList<>();
        operations.add("read");
        scopesWithOperations.setOperation(operations);
        List<String> scopes = new ArrayList<>();
        scopes.add("historicdata");
        scopesWithOperations.setScope(scopes);

        Response response = service.addPermission(user.getId(), "sid1", scopesWithOperations);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void addPermission_whenPermissionDoesNotExists_thenReturnBadRequest() {
        given(userDaoMock.getById(Mockito.anyString())).willReturn(userToMock);

        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        ScopesWithOperations scopesWithOperations = new ScopesWithOperations();
        List<String> operations = new ArrayList<>();
        operations.add("read");
        scopesWithOperations.setOperation(operations);
        List<String> scopes = new ArrayList<>();
        scopes.add("historicdata");
        scopesWithOperations.setScope(scopes);

        Response response = service.addPermission(user.getId(), "sid1", scopesWithOperations);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPermission_successfullyGetPermissions() {
        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(Collections.EMPTY_LIST);

        Response response = service.getPermission(user.getId(), "sid1");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePermission_successfullyDeletePermission() {
        PermissionTO permissionTO = new PermissionTO("permissionId", "sensor:sid1:historicdata:read");
        List<PermissionTO> permissions = new ArrayList<>();
        permissions.add(permissionTO);

        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(permissions);

        given(userDaoMock.getById(Mockito.anyString())).willReturn(user);

        Response response = service.deletePermission("id1", "permissionId");

        verify(userDaoMock).update(user);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePermission_returnsBadRequestBecauseThePermissionIsNotAssignedToTheUser() {
        UserTO user = new UserTO();
        user.setId("id1");
        user.setPassword(USER_PASSWD);
        user.setRoles(Collections.EMPTY_LIST);
        user.setPermissions(new ArrayList<>());

        given(userDaoMock.getById(Mockito.anyString())).willReturn(user);

        Response response = service.deletePermission("id1", "permissionId");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

}
