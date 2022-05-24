package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.Hasher;
import de.urbanpulse.urbanpulsecontroller.admin.RoleManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.binary.Base64;

/**
 * REST Web Service for login to Keycloak
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class LoginRestService extends AbstractRestService {

    private static final Logger LOGGER = Logger.getLogger(LoginRestService.class.getName());

    private static final String BASIC_AUTH_PREFIX = "Basic ";

    @Inject
    private UserManagementDAO dao;

    @Inject
    private RoleManagementDAO roleManagementDAO;


    private WebTarget webTarget;

    @PostConstruct
    public void init() {
        Client client = ClientBuilder.newClient();
    }

    /**
     * Login to Keycloak
     * If the Keycloak authentication was successful
     * then check that the user is persisted to UP or not (if not then it will persist the user)
     *
     * @param authHeader Authentication header
     * @return with the access_token (Bearer token) from Keycloak or with an Unauthorized response
     */
    public Response login(@HeaderParam("Authorization") String authHeader) {
        // At this point we have already passed the basic auth procedure,
        // we just use the auth header to extract the username, so we can fetch the UserTO
        // and return it.

        String base64UsernameColonPw = authHeader.substring(BASIC_AUTH_PREFIX.length());
        String usernameColonPassword = new String(Base64.decodeBase64(base64UsernameColonPw));

        String[] split = usernameColonPassword.split(":");
        String username = split[0];
        String password = split[1];

        Form keycloakTokenGatheringForm = createKeycloakTokenGatheringForm(username, password);
        Entity<Form> entity = Entity.entity(keycloakTokenGatheringForm, MediaType.APPLICATION_FORM_URLENCODED);
        //String authorizationHeaderValue = createAuthHeader();

        // Getting the Access token for the user from keycloak based on the keycloak configuration
        Response response = webTarget
                .request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                //.header("Authorization", authorizationHeaderValue)
                .post(entity);

        //Response should contain an access token if the user is registered to Keycloak
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            List<UserTO> users = dao.getFilteredBy("name", username);
            if (users.isEmpty()) {
                LOGGER.log(Level.INFO,"User {0} is present on Keycloak, but not on UrbanPulse. Adding the new user...",username);
                try {
                    dao.createUser(generateUserTOWithAppUserRole(username, password));
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.severe(e.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            return Response.ok(cleanUnnecessaryFields(responseString).encode()).build();
        } else {
            LOGGER.log(Level.SEVERE, "Keycloak responded with non 200 status! Reason {0} with code {1}",
                    new Object[]{ response.getStatusInfo(), response.getStatus()});
            // We have removed the shiro filters for this endpoint
            // so that keycloak user (not in shiro) can create tokens.
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    private JsonObject cleanUnnecessaryFields(String responseString){
        JsonObject responseJson = new JsonObject(responseString);
        responseJson.remove("refresh_token");
        responseJson.remove("refresh_expires_in");
        responseJson.remove("session_state");
        responseJson.remove("scope");

        return responseJson;
    }

  /*  private String createAuthHeader() {
        return BASIC_AUTH_PREFIX + java.util.Base64.getEncoder().encodeToString((keyCloakConfigLookUp.getKeyCloakClientId() + ":" + keyCloakConfigLookUp.getKeyCloakSecret()).getBytes());
    }*/

    private Form createKeycloakTokenGatheringForm(String username, String password) {
        return new Form()
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password");
    }

    private UserTO generateUserTOWithAppUserRole(String username, String password) throws NoSuchAlgorithmException {
        List<RoleTO> existingRoleTOList = roleManagementDAO.getAll();
        Optional<RoleTO> appUserRole = existingRoleTOList.stream().filter(role -> role.getName().equals("appUser")).findFirst();
        List<RoleTO> roleTOList = new ArrayList<>();
        appUserRole.ifPresent(roleTOList::add);
        return new UserTO(username, password, Hasher.generateRandomHmacSha256Key(), roleTOList, new ArrayList<>());
    }
}
