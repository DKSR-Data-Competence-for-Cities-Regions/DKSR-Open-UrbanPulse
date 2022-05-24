package de.urbanpulse.urbanpulsemanagement.restfacades;

import io.swagger.annotations.ApiOperation;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractRestFacade {

    @Context
    protected UriInfo context;

    @Context
    protected SecurityContext securityContext;

    @OPTIONS
    @ApiOperation(
            value = "Return empty response to OPTIONS request. Used to respond to CORS preflight with Access-Control-*",
            hidden = true
    )
    public Response corsPreflightResponder() {
        return Response.noContent().build();
    }
}
