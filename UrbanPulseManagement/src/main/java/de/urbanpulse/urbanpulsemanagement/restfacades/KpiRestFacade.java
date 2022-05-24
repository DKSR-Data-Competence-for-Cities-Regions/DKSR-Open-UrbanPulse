package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsemanagement.services.KpiRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsemanagement.restfacades.KpiRestFacade.ROOT_PATH;

/**
 * REST Web Service for system health and performance KPIs
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path(ROOT_PATH)
@Api(tags = {"health status", "kpi"})
public class KpiRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "kpi";

    @EJB
    private KpiRestService service;

    @RequiresRoles(ADMIN)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(
            value = "query health status of UrbanPulse"
    )

    public Response getKpiOverview(@QueryParam("refresh") Integer refresh) {
        return service.getKpiOverview(refresh);
    }
}
