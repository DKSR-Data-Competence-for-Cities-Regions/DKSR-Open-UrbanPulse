package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorExtendedTo;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import de.urbanpulse.urbanpulsemanagement.services.VirtualSensorsRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsemanagement.restfacades.VirtualSensorsRestFacade.ROOT_PATH;

import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import io.vertx.core.json.Json;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

@Api(tags = "virtualsensor")
@Path(ROOT_PATH)
public class VirtualSensorsRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "virtualsensors";

    @EJB
    private VirtualSensorsRestService service;

    @RequiresRoles(ADMIN)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve all registered virtual sensors by category id and statement name",
            response = VirtualSensorTO.class, responseContainer = "List")
    public Response getVirtualSensors(@QueryParam("category") String catgeoryId,
                                      @QueryParam("resultStatementName") String resultStatementName) {
        return service.getVirtualSensors(catgeoryId, resultStatementName);
    }

    @RequiresRoles(ADMIN)
    @GET
    @Path("/{sid}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve a registered virtual sensor by its id", response = VirtualSensorTO.class)
    public Response getVirtualSensor(@PathParam("sid") String sid) {
        return service.getVirtualSensor(sid);
    }

    @RequiresRoles(ADMIN)
    @DELETE
    @Path("/{sid}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "delete a registered virtual sensor by id", code = 204)
    public Response deleteVirtualSensor(@PathParam("sid") String sid) {
        return service.deleteVirtualSensor(sid);
    }

    @RequiresRoles(ADMIN)
    @POST
    @Consumes("application/json")
    @ApiOperation(value = "create a virtual sensor")
    public Response createVirtualSensor(VirtualSensorExtendedTo jsonString) {
        try {
            return service.createVirtualSensor(Json.encode(jsonString), context, this);
        } catch (WrappedWebApplicationException e) {
            throw e.getWebApplicationException();
        }
    }

    @RequiresRoles(ADMIN)
    @PUT
    @Path("/{sid}/targets")
    @Consumes("application/json")
    @ApiOperation(value = "update a virtual sensor's targets array")
    public Response updateVirtualSensorTargets(@PathParam("sid") String sid, JsonObject targets) {
        try {
            return service.updateVirtualSensorTargets(sid, targets, context, this);
        } catch (WrappedWebApplicationException e) {
            throw e.getWebApplicationException();
        }
    }
}
