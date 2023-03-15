package de.urbanpulse.urbanpulsemanagement.restfacades;


import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "custom")
@Path("backchannel")
public class ElasticController extends AbstractRestFacade {


    @GET
    @Produces("application/json" + "; charset=utf-8")
    public Response getContents() {
        return Response.ok("REST CONTROLLER BACKCHANNEL").build();
    }
}
