package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapperException;
import de.urbanpulse.urbanpulsemanagement.transfer.SensorsWrapperTO;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.*;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import java.io.StringReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service for registering sensors
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class SensorsRestService extends AbstractRestService {

    private static final Logger LOGGER = Logger.getLogger(SensorsRestService.class.getName());

    @Inject
    private SensorManagementDAO sensorDao;

    @Inject
    private CategoryManagementDAO categoryDao;

    @Inject
    private EventTypeManagementDAO eventTypeDao;

    @Inject
    private SensorModuleUpdateWrapper sensorModuleUpdateWrapper;

    /**
     * retrieve registered sensors (all or those within a category and optionally filtered by comma separated list)
     *
     * @param categoryId category ID string (if this is null all sensors are returned)
     * @param filterBySensors List of strings of sensorIDs
     * @return sensors wrapped in JSON object
     */
    public Response getSensors(String categoryId, List<String> filterBySensors) {
        List<SensorTO> sensors;
        if (categoryId == null) {
            sensors = sensorDao.getAllWithDepsFetched(filterBySensors);
        } else {
            CategoryEntity category = categoryDao.queryById(categoryId);
            sensors = sensorDao.getAllFromCategoryWithDeps(category, filterBySensors);
        }

        SensorsWrapperTO wrapper = new SensorsWrapperTO(sensors);
        return Response.ok(wrapper.toJson().encode()).build();
    }

    /**
     * delete a registered sensor via its ID
     *
     * @param id sensor ID
     * @return 204 NO CONTENT or 202 Accepted (if failed to unregister in Inbound)
     */
    public Response deleteSensor(String id) {

        sensorDao.deleteById(id);

        try {
            sensorModuleUpdateWrapper.unregisterSensor(id);
        } catch (SensorModuleUpdateWrapperException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Method to retrieve a sensor by it's id
     *
     * @param id the id of the sensor that should be retrieved
     * @return 200 OK and the sensor JSON representation as body, or 404 NOT FOUND with
     * an empty JSON object in the response body (if the sensor with the requested id does not exist)
     */
    public Response getSensorById(String id) {
        SensorTO sensor = sensorDao.getById(id);
        if (null == sensor) {
            return ErrorResponseFactory.fromStatus(Response.Status.NOT_FOUND,
                    "sensor with id [" + id + "] does not exist");
        }

        return Response.ok(sensor.toJson().encodePrettily()).build();
    }

    /**
     * create a new sensor registration
     *
     * @param jsonString JSON string with eventtype, senderid, categories (array), description and location
     * fields
     * @param context used to get base builder for the URI
     * @param facade the REST facade
     * @return 201 CREATED or 202 Accepted (if failed to register in Inbound)
     */
    public Response createSensor(String jsonString, UriInfo context, AbstractRestFacade facade) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        Optional<Response> errorResponse = getErrorResponseForMissingElements(HTTP_STATUS_UNPROCESSABLE_ENTITY, jsonObject);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        errorResponse = validateEntityType(jsonObject);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        return createSensorInternal(jsonObject, context, facade);
    }

    /**
     * To validate Event type of sensor
     *
     * @param jsonObject JSON string with event type, sender id, categories (array), description and location
     * @return Optional.ofNullable with status code 422 if no event type exists for the referenced id or is invalid; status code 400 if the eventTypeConfig
     * is empty or doesn't contain the fields SID and timestamp - or null if the eventtype can be used.
     */
    private Optional<Response> validateEntityType(JsonObject jsonObject) {
        Response response;
        final String eventTypeId = jsonObject.getString("eventtype");
        final String MANDATORY_FIELD_SID = "SID";
        final String MANDATORY_FIELD_TIMESTAMP = "timestamp";
        EventTypeEntity eventTypeEntity;

        eventTypeEntity = eventTypeDao.queryById(eventTypeId);
        if (eventTypeEntity == null) {
            response = Response.status(HTTP_STATUS_UNPROCESSABLE_ENTITY)
                    .entity("sensor references missing Event (via eventtype) with id " + eventTypeId)
                    .build();
        } else {
            String config = eventTypeEntity.getEventParameter();
            if (config == null) {
                response = Response.status(HTTP_STATUS_UNPROCESSABLE_ENTITY)
                        .entity("eventtype doesn't contain config")
                        .build();
            } else {
                JsonObject configjson;
                try (JsonReader jsonReader = Json.createReader(new StringReader(config))) {
                    configjson = jsonReader.readObject();
                }
                if (Objects.isNull(configjson)
                        || (!configjson.containsKey(MANDATORY_FIELD_SID)) || (!configjson.containsKey(MANDATORY_FIELD_TIMESTAMP))
                        || configjson.isNull(MANDATORY_FIELD_SID) || configjson.isNull(MANDATORY_FIELD_TIMESTAMP)) {
                    response = Response.status(Response.Status.BAD_REQUEST)
                            .entity("The referenced eventtype has missing key SID and/or timestamp.")
                            .build();
                } else {
                    response = null;
                }
            }
        }

        return Optional.ofNullable(response);

    }

    private Optional<Response> getErrorResponseForMissingElements(int httpStatus, JsonObject jsonObject) {
        Response response;
        if (!jsonObject.containsKey("senderid")) {
            response = Response.status(httpStatus).entity("senderid missing").build();
        } else if (!jsonObject.containsKey("eventtype")) {
            if (jsonObject.containsKey("eventtypes")) {
                response = Response.status(httpStatus)
                        .entity("eventtype missing; multiple eventtypes are no longer supported")
                        .build();
            } else {
                response = Response.status(httpStatus).entity("eventtype missing").build();
            }
        } else if (!jsonObject.containsKey("categories")) {
            response = Response.status(httpStatus).entity("categories missing").build();
        } else if (!jsonObject.containsKey("description")) {
            response = Response.status(httpStatus).entity("description missing").build();
        } else if (!jsonObject.containsKey("location")) {
            response = Response.status(httpStatus).entity("location missing").build();
        } else {
            response = null;
        }
        return Optional.ofNullable(response);
    }

    private Response createSensorInternal(JsonObject jsonObject, UriInfo context, AbstractRestFacade facade)
            throws UriBuilderException, IllegalArgumentException {
        final String eventTypeId = jsonObject.getString("eventtype");

        final JsonArray categories = jsonObject.getJsonArray("categories");
        final List<String> categoryIds = new LinkedList<>();
        categories.getValuesAs(JsonString.class).stream().forEach((category) -> {
            categoryIds.add(category.getString());
        });

        JsonObject description = jsonObject.getJsonObject("description");
        String location = jsonObject.getJsonObject("location").toString();
        String connectorId = jsonObject.getString("senderid");

        SensorTO createdSensor;
        EventTypeEntity eventTypeEntity;
        try {
            eventTypeEntity = eventTypeDao.queryById(eventTypeId);
            createdSensor = sensorDao.createSensor(eventTypeEntity, connectorId, categoryIds, description.toString(), location);
        } catch (ReferencedEntityMissingException ex) {
            return ErrorResponseFactory.badRequest(ex.getMessage());
        }

        if (null == createdSensor) {
            return ErrorResponseFactory.internalServerError("failed to create sensor");
        }

        try {
            sensorModuleUpdateWrapper.registerSensor(createdSensor.getId(), eventTypeEntity.getName(), connectorId);
        } catch (SensorModuleUpdateWrapperException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
        }

        URI sensorUri = getItemUri(context, facade, createdSensor.getId());
        return Response.created(sensorUri).build();
    }

    /**
     * update a sensor
     *
     * @param id ID string of the sensor
     * @param jsonString JSON string with fields to be updated
     * @return 204 NO CONTENT or 202 Accepted (if failed to update in Inbound)
     */
    public Response updateSensor(String id, String jsonString) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        Optional<Response> errorResponse = getErrorResponseForMissingElements(HTTP_STATUS_UNPROCESSABLE_ENTITY, jsonObject);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        errorResponse = validateEntityType(jsonObject);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        return updateSensorInternal(jsonObject, id);
    }

    private Response updateSensorInternal(JsonObject jsonObject, String id) {
        final String eventTypeId = jsonObject.getString("eventtype");

        final JsonArray categories = jsonObject.getJsonArray("categories");
        final List<String> categoryIds = new LinkedList<>();
        categories.getValuesAs(JsonString.class).stream().forEach((category) -> {
            categoryIds.add(category.getString());
        });

        JsonObject description = jsonObject.getJsonObject("description");
        String location = jsonObject.getJsonObject("location").toString();
        String connectorId = jsonObject.getString("senderid");

        SensorTO updatedSensor;
        try {
            updatedSensor = sensorDao.updateSensor(id, eventTypeId, connectorId, categoryIds, description.toString(), location);
            if (null == updatedSensor) {
                return ErrorResponseFactory.internalServerError("failed to update sensor with id[" + id + "]");
            }
        } catch (OperationNotSupportedException ex) {
            return ErrorResponseFactory.internalServerError("sensor to update with id[" + id + "] not found");
        } catch (ReferencedEntityMissingException ex) {
            return ErrorResponseFactory.internalServerErrorFromException(ex);
        }

        EventTypeEntity eventTypeEntity;
        eventTypeEntity = eventTypeDao.queryById(eventTypeId);

        try {
            sensorModuleUpdateWrapper.updateSensor(id, eventTypeEntity.getName());
        } catch (SensorModuleUpdateWrapperException ex) {
            // something went wrong in the inbound module
            LOGGER.log(Level.SEVERE, null, ex);
            return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    public Response getSensorsBySchema(String schemaName) {
        List<EventTypeTO> eventtypes = eventTypeDao.getFilteredBy("name", schemaName);
        if (eventtypes.isEmpty()) {
            return Response.ok(new SensorsWrapperTO().toJson().encode()).build();
        } else {
            String eventTypeId = eventtypes.get(0).getId();
            return Response.ok(new SensorsWrapperTO(sensorDao.getSensorsByEventType(eventTypeId)).toJson().encode()).build();
        }
    }
}
