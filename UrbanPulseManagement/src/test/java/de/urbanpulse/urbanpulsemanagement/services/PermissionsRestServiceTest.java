package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.PermissionsRestFacade;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissionsRestServiceTest {
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/users/42");

    private static final PermissionTO PERMISSION_NONADMIN = new PermissionTO("42", "Nose picking");
    private static final PermissionTO PERMISSION_ADMIN = new PermissionTO("4711", "Delete everything");
    private static final String NON_EXISTING_PERMISSION_ID = "0815";

    @Mock(name = "dao")
    private PermissionManagementDAO permissionDaoMock;

    @Mock(name = "context")
    protected UriInfo contextMock;

    @Mock
    private PermissionsRestFacade permissionsRestFacadeMock;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    PermissionTO permissionToMock;

    @InjectMocks
    private PermissionsRestService service;

    @Before
    public void setUp() {
        given(permissionDaoMock.getById(PERMISSION_NONADMIN.getId())).willReturn(PERMISSION_NONADMIN);
        given(permissionDaoMock.getById(NON_EXISTING_PERMISSION_ID)).willReturn(null);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetPermissions_returnsExpected() throws Exception {
        List<PermissionTO> permissions = new ArrayList<>();
        permissions.add(permissionToMock);

        given(permissionDaoMock.getAll()).willReturn(permissions);

        Response response = service.getPermissions();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetPermissions_returnsEmpty() throws Exception {
        given(permissionDaoMock.getAll()).willReturn(null);

        Response response = service.getPermissions();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetPermission_returnsExpected() throws Exception {
        Response response = service.getPermission(PERMISSION_NONADMIN.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetUser_permissionDoesNotExist() throws Exception {
        Response response = service.getPermission(NON_EXISTING_PERMISSION_ID);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreatePermission_returnsExpected() throws Exception {
        given(permissionDaoMock.createPermission(any())).willReturn(PERMISSION_NONADMIN);

        given(contextMock.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        PermissionTO permission = new PermissionTO();

        permission.setName("Permission to panic");

        Response response = service.createPermission(permission, contextMock, permissionsRestFacadeMock);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeletePermission_returnsExpected() throws Exception {
        Response response = service.deletePermission(PERMISSION_NONADMIN.getId());

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(permissionDaoMock).deleteById(PERMISSION_NONADMIN.getId());
    }
}
