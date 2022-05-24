package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginRestServiceTest {

    @Mock
    private UserManagementDAO userDaoMock;

    @Mock
    private RoleManagementDAO roleManagementDaoMock;



    @Mock
    private WebTarget webTarget;

    @Mock
    private Response responseOk;

    @InjectMocks
    private LoginRestService loginRestService;

    @Test
    public void testLogin_when_keycloak_response_is_unauthorized() {

        Invocation.Builder invocationBuilder = Mockito.mock(Invocation.Builder.class);
        Mockito.when(webTarget.request(any(MediaType.class))).thenReturn(invocationBuilder);

        Invocation.Builder invocationBuilder2 = Mockito.mock(Invocation.Builder.class);
        Mockito.when(invocationBuilder.accept(anyString())).thenReturn(invocationBuilder2);

        Invocation.Builder invocationBuilder3 = Mockito.mock(Invocation.Builder.class);
        Mockito.when(invocationBuilder2.header(anyString(), anyString())).thenReturn(invocationBuilder3);

        Response unauthorizedResponse = Response.status(Response.Status.UNAUTHORIZED).build();
        Mockito.when(invocationBuilder3.post(any(Entity.class))).thenReturn(unauthorizedResponse);

        Response response = loginRestService.login("authorization: Basic dDpBc2QxMjMh");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testLogin_when_keycloak_response_ok() {
        Invocation.Builder invocationBuilder = Mockito.mock(Invocation.Builder.class);
        Mockito.when(webTarget.request(any(MediaType.class))).thenReturn(invocationBuilder);

        Invocation.Builder invocationBuilder2 = Mockito.mock(Invocation.Builder.class);
        Mockito.when(invocationBuilder.accept(anyString())).thenReturn(invocationBuilder2);

        Invocation.Builder invocationBuilder3 = Mockito.mock(Invocation.Builder.class);
        Mockito.when(invocationBuilder2.header(anyString(), anyString())).thenReturn(invocationBuilder3);

        JsonObject keycloakResponse = new JsonObject()
                .put("access_token","12a")
                .put("refresh_token","13a")
                .put("refresh_expires_in",1222);

        Mockito.when(responseOk.readEntity(String.class)).thenReturn(keycloakResponse.encode());
        Mockito.when(responseOk.getStatus()).thenReturn(200);

        Mockito.when(invocationBuilder3.post(any(Entity.class))).thenReturn(responseOk);

        Response response = loginRestService.login("authorization: Basic dDpBc2QxMjMh");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
