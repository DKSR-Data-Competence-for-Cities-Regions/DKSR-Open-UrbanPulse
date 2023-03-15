package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.services.SensorsRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.*;
import static de.urbanpulse.urbanpulsemanagement.restfacades.SensorsRestFacade.ROOT_PATH;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import java.util.Arrays;
import java.util.List;

/**
 * REST Web Service for registering sensors
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "sensor")
@Path(ROOT_PATH)
public class SensorsRestFacade extends AbstractRestFacade {

    public static final String ROOT_PATH = "sensors";

    @EJB
    private SensorsRestService service;

    /**
     * retrieve registered sensors (all or those within a category) or filtered by a comma separated list
     *
     * @param categoryId    category ID string (if this is null all sensors are returned)
     * @param schemaName the name of an event type / schema
     * @param listOfSensors (optional) comma separated string of sensorIDs
     * @return sensors wrapped in JSON object
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve all registered sensors filtered by either category id or schema", response = SensorTO.class, responseContainer = "List")
    public Response getSensors(@QueryParam("category") String categoryId, @QueryParam("schema") String schemaName, @QueryParam("sids") String listOfSensors) {
        List<String> sidFilterList = null;
        
        if(schemaName != null && categoryId != null) {
            return ErrorResponseFactory.badRequest("Sensors can be queried either by category or by schema.");
        }
        
        if (listOfSensors != null && !listOfSensors.isEmpty()) {
            sidFilterList = Arrays.asList(listOfSensors.split(","));
        }
            
        if(schemaName != null) {
            return service.getSensorsBySchema(schemaName);
        }
     
        return service.getSensors(categoryId, sidFilterList);
    }

    /**
     * delete a registered sensor via its ID
     *
     * @param id sensor ID
     * @return 204 NO CONTENT
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER}, logical = Logical.OR)
    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "delete a registered sensor specified by its id")
    public Response deleteSensor(@PathParam("id") String id) {
        return service.deleteSensor(id);
    }

    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "get a registered sensor specified by its id")
    public Response getSensorById(@PathParam("id") String id) {
        return service.getSensorById(id);
    }

    /**
     * create a new sensor registration
     *
     * @param sensorJson JSON string with eventtype, senderid, categories (array), description and location fields
     * @return 201 CREATED
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, CONNECTOR}, logical = Logical.OR)
    @POST
    @Consumes("application/json")
    @ApiOperation(value = "register a new sensor")
    public Response createSensor(String sensorJson) {
        return service.createSensor(sensorJson, context, this);
    }

    /**
     * update a sensor
     *
     * @param id         ID string of the sensor
     * @param sensorJson JSON string with fields to be updated
     * @return 204 NO CONTENT
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, CONNECTOR}, logical = Logical.OR)
    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @ApiOperation(value = "updates an already existing sensor")
    public Response updateSensor(@PathParam("id") String id, String sensorJson) {
        return service.updateSensor(id, sensorJson);
    }
}
