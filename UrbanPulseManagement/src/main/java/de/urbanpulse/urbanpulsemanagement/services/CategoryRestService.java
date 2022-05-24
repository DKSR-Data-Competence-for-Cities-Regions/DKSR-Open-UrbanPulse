package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsemanagement.transfer.CategoriesWrapperTO;
import de.urbanpulse.urbanpulsemanagement.transfer.CategoriesWithChildrenWrapperTO;
import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.CyclicDependencyException;
import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.transfer.SensorsWrapperTO;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service for registering categories
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class CategoryRestService extends AbstractRestService {

    @EJB
    private CategoryManagementDAO categoryDao;

    @EJB
    private SensorManagementDAO sensorDao;

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
    public Response getCategories(String sensorId, String name, Boolean onlyRoots, Boolean resolveChildren) {
        int parameters = 0;
        parameters += (sensorId == null) ? 0 : 1;
        parameters += (name == null) ? 0 : 1;
        if (parameters > 1) {
            return ErrorResponseFactory.badRequest("supported filter params are either 'sensor' or 'name' or neither");
        }

        if ((onlyRoots == null) && (resolveChildren != null)) {
            return ErrorResponseFactory
                    .badRequest("resolve children can only be used in combination with the 'onlyRoot' parameter set tot true!");
        }

        Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "onlyRoots=={0}", onlyRoots);
        if ((null != onlyRoots) && (onlyRoots == true)) {
            return this.getRootCategories(resolveChildren);
        }

        List<CategoryTO> categories = new ArrayList<>();

        if (sensorId == null && name == null) {
            Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "getAll Categories");
            categories = categoryDao.getAll();
            Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "getAll Categories with {0} results",
                    categories.size());
        } else if (sensorId != null) {
            categories = getCategoriesBySensorId(sensorId);
        } else if (name != null) {
            categories = categoryDao.getFilteredBy("name", name);
        }

        if ((null != onlyRoots) && (onlyRoots == true)) {
            Iterator<CategoryTO> it = categories.iterator();
            while (it.hasNext()) {
                CategoryTO categoryTO = it.next();
                if (categoryTO.getParentCategory() != null) {
                    it.remove();
                }
            }
        }

        CategoriesWrapperTO wrapper = new CategoriesWrapperTO(categories);
        JsonObject o = wrapper.toJson();
        Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "Categories wrapper with {0} results",
                o.getJsonArray("categories").size());

        return Response.ok(o).build();
    }

    private List<CategoryTO> getCategoriesBySensorId(String sensorId) {
        List<CategoryTO> categories = new ArrayList<>();
        SensorTO sensor = sensorDao.getById(sensorId);
        if (sensor != null) {
            List<String> categoryIds = sensor.getCategories();
            for (String categoryId : categoryIds) {
                CategoryTO category = categoryDao.getById(categoryId);
                categories.add(category);
            }
        }

        return categories;
    }

    /**
     * Deletes a category and updates dependant relationships
     *
     * @param id the id of the category to delete
     * @return status
     */
    public Response deleteCategory(String id) {
        CategoryTO categoryToDelete = categoryDao.getById(id);
        if (categoryToDelete == null) {
            Response.status(Response.Status.NO_CONTENT).build();
        }

        categoryDao.deleteByIdWithDependencies(id);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * retrieves a category by id
     *
     * @param id the id of the category to get
     * @return the category with the given id or empty if the category does not ecist
     */
    public Response getCategoryById(String id) {
        CategoryTO category = categoryDao.getById(id);
        if (null == category) {
            return ErrorResponseFactory.notFound("category with ID[" + id + "] not found");
        }
        return Response.ok(category.toJson().toString()).build();
    }

    /**
     * register a new category
     *
     * @param jsonString JSON string with name and description fields
     * @param context the application and URI context
     * @param facade the abstract REST facade
     * @return the id of the newly created category
     */
    public Response createCategory(String jsonString, UriInfo context, AbstractRestFacade facade) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        if (!jsonObject.containsKey("name")) {
            return ErrorResponseFactory.unprocessibleEntity("category name missing");
        }
        String name = jsonObject.getString("name");

        if (!jsonObject.containsKey("description")) {
            return ErrorResponseFactory.unprocessibleEntity("category description missing");
        }
        JsonObject description = jsonObject.getJsonObject("description");

        List<String> childCategoryIds = new LinkedList<>();
        JsonArray childCategories = jsonObject.getJsonArray("childCategories");

        if (childCategories != null) {
            for (JsonValue categoryId : childCategories) {
                String idString = ((JsonString) categoryId).getString();
                childCategoryIds.add(idString);
            }
        }

        final boolean categoryWithNameExists = categoryDao.getFilteredBy("name", name).size() > 0;
        if (categoryWithNameExists) {
            return ErrorResponseFactory.conflict("category with name [" + name + "] already exist");
        }

        try {
            CategoryTO createdCategory =
                    categoryDao.createCategory(name, description.toString(), childCategoryIds);
            URI categoryUri = getItemUri(context, facade, createdCategory.getId());
            return Response.created(categoryUri).build();
        } catch (ReferencedEntityMissingException | CyclicDependencyException ex) {
            return ErrorResponseFactory.badRequest(ex.toString());
        }
    }

    /**
     * updates an already existing category
     *
     * @param id of the category to update
     * @param jsonString JSON string with an optional name, description and array of child categories to update
     * @return status of the update request
     */
    public Response updateCategory(String id, String jsonString) {

        CategoryTO categoryToUpdate = categoryDao.getById(id);
        if (categoryToUpdate == null) {
            return ErrorResponseFactory.badRequest("category to update with id [" + id + "] does not exist");
        }

        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        String name = null;
        if (jsonObject.containsKey("name")) {
            name = jsonObject.getString("name");
        }

        String description = null;
        if (jsonObject.containsKey("description")) {
            JsonObject descriptionObject = jsonObject.getJsonObject("description");
            description = descriptionObject.toString();
        }

        List<String> childCategoryIds = new LinkedList<>();
        JsonArray childCategories = null;
        if (jsonObject.containsKey("childCategories")) {
            childCategories = jsonObject.getJsonArray("childCategories");
        }

        if (childCategories != null && childCategories.size() > 0) {
            for (JsonValue categoryId : childCategories) {
                String idString = ((JsonString) categoryId).getString();
                childCategoryIds.add(idString);
            }
        }

        try {
            categoryDao.updateCategory(categoryToUpdate, name, description, childCategoryIds);
            return Response.noContent().build();
        } catch (ReferencedEntityMissingException | CyclicDependencyException | OperationNotSupportedException ex) {
            return ErrorResponseFactory.badRequest(ex.toString());
        }
    }

    /**
     * Retrieves all child categories of a given parent
     *
     * @param parentId the parent id
     * @param resolveChildren resolve the retrieved categories to deliver also all it's children.
     * @return all children or en empty array if there are none
     */
    public Response getChildCategories(String parentId, Boolean resolveChildren) {
        CategoryTO parentCategory = categoryDao.getById(parentId);
        if (parentCategory == null) {
            return ErrorResponseFactory.notFound("category with Id [" + parentId + "] does not exist");
        }

        if ((resolveChildren != null) && (resolveChildren == true)) {
            List<CategoryWithChildrenTO> children = categoryDao.getFullyResolvedChildCategories(parentCategory);
            CategoriesWithChildrenWrapperTO wrapper = new CategoriesWithChildrenWrapperTO(children);
            return Response.ok(wrapper.toJson().toString()).build();
        } else {
            List<CategoryTO> children = categoryDao.getChildCategories(parentCategory);
            CategoriesWrapperTO wrapper = new CategoriesWrapperTO(children);
            return Response.ok(wrapper.toJson().toString()).build();
        }
    }

    /**
     * Retrieve all root categories
     *
     * @param resolveChildren If set to true the childCategories arry will be fully populated with the according children instead of
     *        only giving back their child Id's.
     * @return all root categories
     */
    public Response getRootCategories(Boolean resolveChildren) {
        if ((resolveChildren != null) && (resolveChildren == true)) {
            List<CategoryWithChildrenTO> rootCategories = categoryDao.getFullyResolvedRootCategories();
            Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "rootCategories with {0} results",
                    rootCategories.size());
            CategoriesWithChildrenWrapperTO wrapper = new CategoriesWithChildrenWrapperTO(rootCategories);
            io.vertx.core.json.JsonObject json = wrapper.toJson();

            Logger.getLogger(CategoryRestService.class.getName()).log(Level.INFO, "rootCategories json array with {0} results",
                    json.getJsonArray("categories").size());
            return Response.ok(json.encode()).build();
        } else {
            List<CategoryTO> rootCategories = categoryDao.getRootCategories();
            CategoriesWrapperTO wrapper = new CategoriesWrapperTO(rootCategories);
            return Response.ok(wrapper.toJson().toString()).build();
        }
    }

    /**
     * Retrieves the parent of a given category
     *
     * @param id the id of the category to get the parent for
     * @return the parent category or empty if none exist
     */
    public Response getParentCategory(String id) {
        final CategoryTO parentCategory = categoryDao.getParentCategory(id);
        if (parentCategory == null) {
            return ErrorResponseFactory.notFound("category with id [" + id + "] does not exist or lacks parent");
        }

        return Response.ok(parentCategory.toJson().toString()).build();
    }

    /**
     * Retrieves the parent of a given category
     *
     * @param id the id of the category to get the sensors for
     * @return the sensors of a category
     */
    public Response getSensorsForCategory(String id) {
        Optional<List<SensorTO>> sensors = categoryDao.getSensors(id);
        if (!sensors.isPresent()) {
            return ErrorResponseFactory.notFound("category with Id [" + id + "] does not exist");
        }

        SensorsWrapperTO wrapper = new SensorsWrapperTO(sensors.get());
        return Response.ok(wrapper.toJson().toString()).build();
    }
}
