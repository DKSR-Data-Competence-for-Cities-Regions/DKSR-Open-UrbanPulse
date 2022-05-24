package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.UserWithIds;
import de.urbanpulse.urbanpulsemanagement.services.UsersRestService;
import java.util.Collections;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UsersRestFacadeMockitoTest extends AbstractRestFacadeTest {

    private static final String USER_ID = "userId";
    private static final String USER_NAME = "testRoleName";
    private static final String PERMISSION_ID = "testPermissionId";
    private static final String ROLE_ID = "testRoleId";
    private static final String SID = "sid";

    @Mock
    private UsersRestService service;

    @Mock
    private Response response;

    @Mock
    private UriInfo context;

    @Mock
    private ScopesWithOperations scopesWithOperations;

    @Mock
    private SecurityContext securityContext;

    private UserWithIds userWithIds;

    @InjectMocks
    private final UsersRestFacade usersRestFacade = new UsersRestFacade(service);

    @Before
    public void setUp() {
        userWithIds = new UserWithIds();
        userWithIds.setName(USER_NAME);
        userWithIds.setPermissions(Collections.singletonList(PERMISSION_ID));
        userWithIds.setRoles(Collections.singletonList(ROLE_ID));
    }

    @Test
    public void test_getUsers_willDelegateToService() {
        given(service.getUsers()).willReturn(response);

        Response actualResponse = usersRestFacade.getUsers();

        verify(service, times(1)).getUsers();
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_getUser_willDelegateToService() {
        given(service.getUser(USER_ID)).willReturn(response);

        Response actualResponse = usersRestFacade.getUser(USER_ID);

        verify(service, times(1)).getUser(USER_ID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_createUser_willDelegateToService() {
        ArgumentCaptor<UserTO> userCaptor = ArgumentCaptor.forClass(UserTO.class);
        given(service.createUser(any(UserTO.class), eq(context), eq(usersRestFacade))).willReturn(response);

        Response actualResponse = usersRestFacade.createUser(userWithIds);

        verify(service, times(1)).createUser(userCaptor.capture(), eq(context), eq(usersRestFacade));
        assertEquals(this.response, actualResponse);
        assertEquals(USER_NAME, userCaptor.getValue().getName());
        assertEquals(1, userCaptor.getValue().getPermissions().size());
        assertEquals(PERMISSION_ID, userCaptor.getValue().getPermissions().get(0).getId());
        assertEquals(1, userCaptor.getValue().getRoles().size());
        assertEquals(ROLE_ID, userCaptor.getValue().getRoles().get(0).getId());
    }

    @Test(expected = WebApplicationException.class)
    public void test_createUser_userNull_willThrowWebApplicationException() {
        usersRestFacade.createUser(null);
    }

    @Test
    public void test_resetKey_willDelegateToService() {
        given(service.resetKey(eq(USER_ID), eq(securityContext))).willReturn(response);

        Response actualResponse = usersRestFacade.resetKey(USER_ID);

        verify(service, times(1)).resetKey(eq(USER_ID), eq(securityContext));
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_update_willDelegateToService() {
        ArgumentCaptor<UserTO> userCaptor = ArgumentCaptor.forClass(UserTO.class);
        given(service.updateUser(eq(USER_ID), any(UserTO.class), eq(securityContext))).willReturn(response);

        Response actualResponse = usersRestFacade.update(USER_ID, userWithIds);

        verify(service, times(1)).updateUser(eq(USER_ID), userCaptor.capture(), eq(securityContext));
        assertEquals(this.response, actualResponse);
        assertEquals(USER_NAME, userCaptor.getValue().getName());
        assertEquals(1, userCaptor.getValue().getPermissions().size());
        assertEquals(PERMISSION_ID, userCaptor.getValue().getPermissions().get(0).getId());
        assertEquals(1, userCaptor.getValue().getRoles().size());
        assertEquals(ROLE_ID, userCaptor.getValue().getRoles().get(0).getId());
    }

    @Test
    public void test_deleteUser_willDelegateToService() {
        given(service.deleteUser(USER_ID)).willReturn(response);

        Response actualResponse = usersRestFacade.deleteUser(USER_ID);

        verify(service, times(1)).deleteUser(USER_ID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_addPermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.addPermission(USER_ID, SID, scopesWithOperations)).willReturn(response);

        Response actualResponse = usersRestFacade.addPermission(USER_ID, SID, scopesWithOperations);
        verify(service, times(1)).addPermission(USER_ID, SID, scopesWithOperations);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_addPermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = usersRestFacade.addPermission(USER_ID, SID, scopesWithOperations);

        verify(service, never()).addPermission(anyString(), anyString(), any(ScopesWithOperations.class));
        assertEquals(403, actualResponse.getStatus());
    }

    @Test
    public void test_getPermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.getPermission(USER_ID, SID)).willReturn(response);

        Response actualResponse = usersRestFacade.getPermission(USER_ID, SID);
        verify(service, times(1)).getPermission(USER_ID, SID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_getPermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = usersRestFacade.getPermission(USER_ID, SID);

        verify(service, never()).getPermission(anyString(), anyString());
        assertEquals(403, actualResponse.getStatus());
    }

    @Test
    public void test_deletePermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.deletePermission(USER_ID, SID)).willReturn(response);

        Response actualResponse = usersRestFacade.deletePermission(USER_ID, SID);
        verify(service, times(1)).deletePermission(USER_ID, SID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_deletePermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = usersRestFacade.deletePermission(USER_ID, SID);

        verify(service, never()).deletePermission(anyString(), anyString());
        assertEquals(403, actualResponse.getStatus());
    }
}
