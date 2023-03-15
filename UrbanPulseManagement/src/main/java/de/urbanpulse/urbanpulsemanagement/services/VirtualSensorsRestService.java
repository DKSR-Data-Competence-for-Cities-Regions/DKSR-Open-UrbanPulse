package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.VirtualSensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.EventTypeException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.transfer.VirtualSensorsWrapperTO;
import de.urbanpulse.urbanpulsemanagement.util.EventTypesRegistrar;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import de.urbanpulse.urbanpulsemanagement.virtualsensors.VirtualSensorsCreator;
import de.urbanpulse.urbanpulsemanagement.virtualsensors.VirtualSensorsErrorResponseFactory;

import java.io.StringReader;
import java.util.*;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class VirtualSensorsRestService extends AbstractRestService {

    @Inject
    private VirtualSensorManagementDAO virtualSensorsDAO;

    @Inject
    private StatementManagementDAO statementManagementDAO;

    @Inject
    private EventTypesRegistrar eventTypesRegistrar;

    @Inject
    private EventProcessorWrapper eventProcessor;

    @Inject
    private VirtualSensorsCreator virtualSensorCreator;

    @Inject
    private VirtualSensorsErrorResponseFactory errorResponseFactory;

    @Inject
    private OutboundInterfacesManagementDAO outboundInterfacesManagementDAO;

    private static final String KEY_TARGETS = "targets";

    public Response getVirtualSensors(String categoryId, String resultStatementName, String schemaName) {
        List<VirtualSensorTO> virtualSensors;
        if (categoryId == null && schemaName == null && resultStatementName == null) {
            virtualSensors = virtualSensorsDAO.getAll();
        } else if (categoryId == null && schemaName == null) {
            virtualSensors = virtualSensorsDAO.getFilteredByResultStatementName(resultStatementName);
        } else if (schemaName == null && resultStatementName == null ) {
            virtualSensors = virtualSensorsDAO.getFilteredByCategory(categoryId);
        }  else if (categoryId == null && resultStatementName == null ) {
            virtualSensors = virtualSensorsDAO.getFilteredBySchema(schemaName);
        } else {
            return ErrorResponseFactory.badRequest("do not specify categoryId, schemaName and/or resultStatementName at the same time");
        }

        VirtualSensorsWrapperTO wrapper = new VirtualSensorsWrapperTO(virtualSensors);
        return Response.ok(wrapper.toJson()).build();
    }
    
    

    /**
     * @param sid the id of the searched sensor
     * @return a response containing the virtual sensor as json
     */
    public Response getVirtualSensor(String sid) {
        VirtualSensorTO virtualSensor = virtualSensorsDAO.getById(sid);
        if (virtualSensor == null) {
            return ErrorResponseFactory.notFound("virtual sensor with sid [" + sid + "] does not exist");
        }

        return Response.ok(virtualSensor.toJson()).build();
    }

    /**
     * @param sid the id of the sensor to be deleted
     * @return an empty response or an ErrorResponseFactory
     */
    public Response deleteVirtualSensor(String sid) {
        VirtualSensorEntity virtualSensor = virtualSensorsDAO.queryById(sid);
        if (virtualSensor == null) {
            return Response.noContent().build();
        }

        List<String> statementIdsToDelete = findStatementsToDelete(virtualSensor);
        if (anyStatementHasUpdateListeners(statementIdsToDelete)) {
            return ErrorResponseFactory.conflict("virtual sensor with SID[" + sid
                    + "] still has listener(s) on its statement(s), cannot delete");
        }

        List<String> eventTypeIdsToDelete = stringListFromJsonArray(Json.createReader(new StringReader(virtualSensor.getEventTypeIds())).readArray());
        if (virtualSensor.getResultEventType() != null) {
            eventTypeIdsToDelete.add(String.valueOf(virtualSensor.getResultEventType().getId()));
        }

        virtualSensorsDAO.deleteById(sid);

        deleteStatements(statementIdsToDelete);
        try {
            deleteEventTypes(eventTypeIdsToDelete);
        } catch (EventTypeException ex) {
            return ErrorResponseFactory.conflict(ex.getLocalizedMessage());
        }

        try {
            eventProcessor.unregisterVirtualSensor(sid);
        } catch (EventProcessorWrapperException ex) {
            return ErrorResponseFactory.conflict(ex.getLocalizedMessage());
        }

        return Response.noContent().build();
    }

    private List<String> findStatementsToDelete(VirtualSensorEntity virtualSensor) {
        String statementIdsJsonString = virtualSensor.getStatementIds();
        JsonArray statementIdsJson = Json.createReader(new StringReader(statementIdsJsonString)).readArray();
        List<String> statementIdsToDelete = stringListFromJsonArray(statementIdsJson);
        Collections.reverse(statementIdsToDelete);
        statementIdsToDelete.add("" + virtualSensor.getResultStatement().getId());
        return statementIdsToDelete;
    }

    private List<String> stringListFromJsonArray(JsonArray jsonArray) {
        List<String> list = new LinkedList<>();
        jsonArray.stream().map((value) -> (JsonString) value).forEach((stringValue) -> {
            list.add(stringValue.getString());
        });
        return list;
    }

    private void deleteEventTypes(List<String> eventTypeIdsToDelete) {
        eventTypeIdsToDelete.forEach(id -> eventTypesRegistrar.deleteEventTypeById(id, false));
    }

    private void deleteStatements(List<String> statementIdsToDelete) {
        for (String statementId : statementIdsToDelete) {
            statementManagementDAO.deleteById(statementId);
        }
    }

    /**
     * @param jsonString a string representation of the configuration for the sensor
     * @param context used to get base builder for the URI
     * @param facade the REST facade
     * @return the new virtual sensor
     * @throws WrappedWebApplicationException event type registration failed
     */
    public Response createVirtualSensor(String jsonString, UriInfo context, AbstractRestFacade facade)
            throws WrappedWebApplicationException {

        JsonObject jsonObject;

        try {
            JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            jsonObject = jsonReader.readObject();
        } catch (JsonException ex) {
            return ErrorResponseFactory.badRequest("Malformed JSON provided");
        }

        Optional<Response> errorResponse = errorResponseFactory.getErrorResponseForMissingElements(jsonObject);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        return virtualSensorCreator.createVirtualSensor(jsonObject, context, facade);
    }

    private boolean anyStatementHasUpdateListeners(List<String> statementIds) {
        for (String id : statementIds) {
            if (!outboundInterfacesManagementDAO.getUpdateListenersOfStatement(id).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public Response updateVirtualSensorTargets(String sid, JsonObject targets, UriInfo context, AbstractRestFacade facade) {
        if (isValidTargetObject(targets)) {
            VirtualSensorEntity virtualSensor = virtualSensorsDAO.queryById(sid);
            String resultStatementName = virtualSensor.getResultStatement().getName();
            JsonArray targetsArray = targets.getJsonArray(KEY_TARGETS);
            return virtualSensorCreator.updateVirtualSensorTargets(sid, resultStatementName, targetsArray.toString(), context, facade);
        } else {
            return ErrorResponseFactory.badRequest("Body not contains targets or not just only targets");
        }
    }

    private boolean isValidTargetObject(JsonObject targetObject) {
        if (targetObject.containsKey(KEY_TARGETS) && targetObject.size() == 1 && targetObject.get(KEY_TARGETS).getValueType().equals(JsonValue.ValueType.ARRAY)) {
            return targetObject.getJsonArray(KEY_TARGETS).stream()
                    .filter(target -> !target.getValueType().equals(JsonValue.ValueType.STRING))
                    .count() == 0;
        }
        return false;
    }
}
