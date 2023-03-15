package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.services.CategoryRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.*;
import static de.urbanpulse.urbanpulsemanagement.restfacades.CategoryRestFacade.ROOT_PATH;


/**
 * REST Web Service for registering categories
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "category")
@Path(ROOT_PATH)
public class CategoryRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "categories";

    @EJB
    private CategoryRestService service;


    /**
     * retrieve registered categories with optional filter by name or sensor ID
     *
     * @param sensorId filter by sensor
     * @param name filter by name
     * @param onlyRoots retrieve only root categories
     * @param resolveChildren resolve the retrieved categories to deliver also all it's children. Does only make sense in the
     *        combination with the "onlyRoots" parameter set to true!
     * @return categories wrapped in JSON object
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve registered categories with optional filter by name or sensor ID", response = CategoryTO.class,
            responseContainer = "List")
    public Response getCategories(@QueryParam("sensor") String sensorId, @QueryParam("name") String name,
            @QueryParam("onlyRoots") Boolean onlyRoots, @QueryParam("resolveChildren") Boolean resolveChildren) {
        return service.getCategories(sensorId, name, onlyRoots, resolveChildren);
    }

    /**
     * Deletes a category and updates dependant relationships
     *
     * @param id the id of the category to delete
     * @return status
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER}, logical = Logical.OR)
    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "deletes a category and updates dependant relationships")
    public Response deleteCategory(@PathParam("id") String id) {
        return service.deleteCategory(id);
    }

    /**
     * retrieves a category by id
     *
     * @param id the id of the category to get
     * @return the category with the given id or empty if the category does not exist
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieves a category by id", response = CategoryTO.class)
    public Response getCategoryById(@PathParam("id") String id) {
        return service.getCategoryById(id);
    }

    /**
     * register a new category
     *
     * @param categoryJson serialized category transfer object with name and description fields
     * @return the id of the newly created category
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, CONNECTOR}, logical = Logical.OR)
    @POST
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(value = "register a new category")
    public Response createCategory(@ApiParam(required = true) String categoryJson) {
        return service.createCategory(categoryJson, context, this);
    }

    /**
     * updates an already existing category
     *
     * @param id of the category to update
     * @param categoryJson serialized category transfer object with an optional name, description and array of child categories to
     *        update
     * @return status of the update request
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, CONNECTOR}, logical = Logical.OR)
    @PUT
    @Path("/{id}")
    @Consumes("application/json" + "; charset=utf-8")
    @ApiOperation(value = "updates an already existing category")
    public Response updateCategory(@PathParam("id") String id, @ApiParam(required = true) String categoryJson) {
        return service.updateCategory(id, categoryJson);
    }

    /**
     * Retrieves all child categories of a given parent
     *
     * @param parentId the parent id
     * @param resolveChildren resolve the retrieved categories to deliver also all it's children.
     * @return all children or en empty array if there are none
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}/children")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieves all child categories of a given parent", response = CategoryTO.class,
            responseContainer = "List")
    public Response getChildCategories(@PathParam("id") String parentId, @QueryParam("resolveChildren") Boolean resolveChildren) {
        return service.getChildCategories(parentId, resolveChildren);
    }

    /**
     * Retrieve all root categories
     *
     * @param resolveChildren If set to true the childCategories array will be fully populated with the according children instead of
     *        only giving back their child Id's.
     * @return all root categories
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/root")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve all root categories", responseContainer = "List")
    public Response getRootCategories(@DefaultValue("false") @QueryParam("resolveChildren") boolean resolveChildren) {
        return service.getRootCategories(resolveChildren);
    }

    /**
     * Retrieves the parent of a given category
     *
     * @param id the id of the category to get the parent for
     * @return the parent category or empty if none exist
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}/parent")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieves the parent of a given category", response = CategoryTO.class)
    public Response getParentCategory(@PathParam("id") String id) {
        return service.getParentCategory(id);
    }

    /**
     * Retrieves the parent of a given category
     *
     * @param id the id of the category to get the sensors for
     * @return the sensors of a category
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}/sensors")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieves the parent of a given category", response = SensorTO.class, responseContainer = "List")
    public Response getSensorsForCategory(@PathParam("id") String id) {
        return service.getSensorsForCategory(id);
    }

}
