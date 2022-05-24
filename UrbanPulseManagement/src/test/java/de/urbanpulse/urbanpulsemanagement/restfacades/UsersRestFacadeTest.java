package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import de.urbanpulse.urbanpulsemanagement.services.UsersRestService;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UsersRestFacadeTest extends JerseyTest {

    private static final MediaType APPLICATION_JSON_UTF_8 = MediaType.valueOf("application/json; charset=utf-8");
    private static final String USERS_ENDPOINT = "users";

    private UsersRestFacade usersRestFacade;
    private UsersRestService mockUsersRestService;

    @Override
    protected Application configure() {
        mockUsersRestService = mock(UsersRestService.class);
        usersRestFacade = new UsersRestFacade(mockUsersRestService);
        return new ResourceConfig().register(usersRestFacade);
    }

    @Test
    public void createUser_ShouldMapRequestToServiceCallAndForwardResponse() throws Exception {
        Response responseCreated = Response.created(URI.create("users/1")).build();
        when(mockUsersRestService.createUser(any(), any(), any())).thenReturn(responseCreated);
        Map<String, Object> userWithRoleIds = new HashMap<>();
        userWithRoleIds.put("name", "Béla");
        userWithRoleIds.put("password", "titok");
        userWithRoleIds.put("roles", asList("role1", "role2"));

        Response response = postUser(userWithRoleIds);

        assertThat(response.getStatus(), equalTo(responseCreated.getStatus()));
        assertThat(response.getLocation(), equalTo(responseCreated.getLocation()));
        UserTO userTO = new UserTO("Béla", "titok", "aSecretKey", asList(new RoleTO("role1"), new RoleTO("role2")), EMPTY_LIST);
        verify(mockUsersRestService, times(1))
                .createUser(UserTOMatcher.eq(userTO), eq(usersRestFacade.context), eq(usersRestFacade));
        verifyNoMoreInteractions(mockUsersRestService);
    }

    @Test
    public void createUser_ShouldCallServiceWithEmptyRoleList_WhenRolesIsMissing() throws Exception {
        Map<String, Object> userWithRoleIds = new HashMap<>();
        userWithRoleIds.put("name", "Béla");
        userWithRoleIds.put("password", "titok");

        postUser(userWithRoleIds);

        UserTO userTO = new UserTO("Béla", "titok", "aSecretKey", EMPTY_LIST, EMPTY_LIST);
        verify(mockUsersRestService, times(1))
                .createUser(UserTOMatcher.eq(userTO), eq(usersRestFacade.context), eq(usersRestFacade));
        verifyNoMoreInteractions(mockUsersRestService);
    }

    @Test
    public void createUser_ShouldRespondWithBadRequest_WhenRolesIsOfWrongType() throws Exception {
        Map<String, Object> userWithRoleIds = new HashMap<>();
        userWithRoleIds.put("name", "Béla");
        userWithRoleIds.put("password", "titok");
        userWithRoleIds.put("roles", "not json array");

        Response response = postUser(userWithRoleIds);

        assertThat(response.getStatus(), equalTo(BAD_REQUEST.getStatusCode()));
        verifyZeroInteractions(mockUsersRestService);
    }

    @Test
    public void createUser_ShouldRespondWithBadRequest_WhenRequestBodyIsMissing() throws Exception {
        Response response = target(USERS_ENDPOINT)
                .request(APPLICATION_JSON_UTF_8)
                .post(Entity.json(""));

        assertThat(response.getStatus(), equalTo(BAD_REQUEST.getStatusCode()));
        verifyZeroInteractions(mockUsersRestService);
    }

    private Response postUser(Map<String, Object> userWithRoleIds) {
        return target(USERS_ENDPOINT)
                .request(APPLICATION_JSON_UTF_8)
                .post(Entity.json(userWithRoleIds));
    }

}
