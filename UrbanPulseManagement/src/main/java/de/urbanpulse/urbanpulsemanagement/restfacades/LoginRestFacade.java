package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsemanagement.services.LoginRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsemanagement.restfacades.LoginRestFacade.ROOT_PATH;

import io.swagger.annotations.ApiParam;

/**
 * REST Web Service for login to Keycloak
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path(ROOT_PATH)
@Api(tags = {"user", "login"})
public class LoginRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "login";

    @EJB
    private LoginRestService service;

    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "check that the user is existing in Keycloak",
            response = Response.class
    )
    public Response login(@HeaderParam("Authorization") @ApiParam(hidden = true) String authHeader) {
        return service.login(authHeader);
    }
}
