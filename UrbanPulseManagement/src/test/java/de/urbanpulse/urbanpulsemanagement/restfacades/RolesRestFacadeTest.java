package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.RoleWithIds;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;
import de.urbanpulse.urbanpulsemanagement.services.RolesRestService;

import java.util.Collections;
import javax.ws.rs.core.Response;
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
public class RolesRestFacadeTest extends AbstractRestFacadeTest {

    private static final String ROLE_ID = "testId";
    private static final String ROLE_NAME = "testRoleName";
    private static final String PERMISSION_ID = "testPermissionId";
    private static final String SID = "sid";

    @Mock
    private RolesRestService service;

    @Mock
    private Response response;

    @Mock
    private UriInfo context;

    @Mock
    private ScopesWithOperations scopesWithOperations;

    private RoleWithIds roleWithIds;

    @InjectMocks
    private RolesRestFacade rolesRestFacade;

    @Before
    public void setUp() {
        roleWithIds = new RoleWithIds();
        roleWithIds.setName(ROLE_NAME);
        roleWithIds.setPermissions(Collections.singletonList(PERMISSION_ID));
    }

    @Test
    public void test_getRoles_willDelegateToService() {
        given(service.getRoles()).willReturn(response);

        Response actualResponse = rolesRestFacade.getRoles();

        verify(service, times(1)).getRoles();
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_getRole_willDelegateToService() {
        given(service.getRole(ROLE_ID)).willReturn(response);

        Response actualResponse = rolesRestFacade.getRole(ROLE_ID);

        verify(service, times(1)).getRole(ROLE_ID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_createRole_willDelegateToService() {
        ArgumentCaptor<RoleTO> roleCaptor = ArgumentCaptor.forClass(RoleTO.class);
        given(service.createRole(any(RoleTO.class), eq(context), eq(rolesRestFacade))).willReturn(response);

        Response actualResponse = rolesRestFacade.createRole(roleWithIds);

        verify(service, times(1)).createRole(roleCaptor.capture(), eq(context), eq(rolesRestFacade));
        assertEquals(this.response, actualResponse);
        assertEquals(ROLE_NAME, roleCaptor.getValue().getName());
        assertEquals(1, roleCaptor.getValue().getPermissions().size());
        assertEquals(PERMISSION_ID, roleCaptor.getValue().getPermissions().get(0).getId());
    }

    @Test
    public void test_updateRole_willDelegateToService() {
        ArgumentCaptor<RoleTO> roleCaptor = ArgumentCaptor.forClass(RoleTO.class);
        given(service.updateRole(eq(ROLE_ID), any(RoleTO.class))).willReturn(response);

        Response actualResponse = rolesRestFacade.updateRole(ROLE_ID, roleWithIds);

        verify(service, times(1)).updateRole(eq(ROLE_ID), roleCaptor.capture());
        assertEquals(this.response, actualResponse);
        assertEquals(ROLE_NAME, roleCaptor.getValue().getName());
        assertEquals(1, roleCaptor.getValue().getPermissions().size());
        assertEquals(PERMISSION_ID, roleCaptor.getValue().getPermissions().get(0).getId());
    }

    @Test
    public void test_deleteRole_willDelegateToService() {
        given(service.deleteRole(ROLE_ID)).willReturn(response);

        Response actualResponse = rolesRestFacade.deleteRole(ROLE_ID);

        verify(service, times(1)).deleteRole(ROLE_ID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_addPermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.addPermission(ROLE_ID, SID, scopesWithOperations)).willReturn(response);

        Response actualResponse = rolesRestFacade.addPermission(ROLE_ID, SID, scopesWithOperations);
        verify(service, times(1)).addPermission(ROLE_ID, SID, scopesWithOperations);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_addPermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = rolesRestFacade.addPermission(ROLE_ID, SID, scopesWithOperations);

        verify(service, never()).addPermission(anyString(), anyString(), any(ScopesWithOperations.class));
        assertEquals(403, actualResponse.getStatus());
    }

    @Test
    public void test_getPermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.getPermission(ROLE_ID, SID)).willReturn(response);

        Response actualResponse = rolesRestFacade.getPermission(ROLE_ID, SID);
        verify(service, times(1)).getPermission(ROLE_ID, SID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_getPermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = rolesRestFacade.getPermission(ROLE_ID, SID);

        verify(service, never()).getPermission(anyString(), anyString());
        assertEquals(403, actualResponse.getStatus());
    }

    @Test
    public void test_deletePermission_allowed_willDelegateToService() {
        given(subject.isPermitted(anyString())).willReturn(true);
        given(service.deletePermission(ROLE_ID, SID)).willReturn(response);

        Response actualResponse = rolesRestFacade.deletePermission(ROLE_ID, SID);
        verify(service, times(1)).deletePermission(ROLE_ID, SID);
        assertEquals(this.response, actualResponse);
    }

    @Test
    public void test_deletePermission_notAllowed_willReturnForbidden() {
        given(subject.isPermitted(anyString())).willReturn(false);

        Response actualResponse = rolesRestFacade.deletePermission(ROLE_ID, SID);

        verify(service, never()).deletePermission(anyString(), anyString());
        assertEquals(403, actualResponse.getStatus());
    }
}
