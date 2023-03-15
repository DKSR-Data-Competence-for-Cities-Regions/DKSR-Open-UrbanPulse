package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsemanagement.services.EPRStatusRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("eprstatus")
@Api(tags = {"event processor", "status"})
public class EPRStatusRestFacade extends AbstractRestFacade {

    @EJB
    private EPRStatusRestService service;

    /**
     * get event processor status
     *
     * @param key currently the only supported value is "events_processed"
     * @return 200 OK with status EPR status JSON like '{ "KEY": SOME_VALUE }' if key is supported, 400 BAD REQUEST if
     * key is unsupported
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @ApiOperation(
            value = "Get the status of the event processor.",
            response = JsonObject.class
    )
    @RequiresRoles(ADMIN)
    public Response getEprStatus(@QueryParam("key") String key) {
        return service.getEprStatus(key);
    }
}
