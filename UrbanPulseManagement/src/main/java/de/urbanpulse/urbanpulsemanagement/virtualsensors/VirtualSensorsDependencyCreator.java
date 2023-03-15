package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.exceptions.FailedToPersistStatementException;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import de.urbanpulse.urbanpulsemanagement.util.WrappedWebApplicationException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class VirtualSensorsDependencyCreator {

    @Inject
    private StatementManagementDAO statementManagementDAO;

    @Inject
    private EventTypeManagementDAO eventTypeManagementDAO;

    @Inject
    private EventProcessorWrapper eventProcessor;

    public javax.json.JsonArray persistEventTypes(List<javax.json.JsonObject> eventTypes) {
        JsonArrayBuilder eventTypeIdsBuilder = Json.createArrayBuilder();
        eventTypes
                .stream()
                .map(this::persistEventType)
                .map(EventTypeTO::getId)
                .forEachOrdered(eventTypeIdsBuilder::add);
        return eventTypeIdsBuilder.build();
    }

    public EventTypeTO persistEventType(javax.json.JsonObject eventType) {
        String name = eventType.getString("name");
        String config = eventType.getJsonObject("config").toString();
        String description = eventType.getJsonObject("description").toString();

        if (eventTypeManagementDAO.eventTypeExists(name)) {
            String errorMessage = "Cannot create EventType with name " + name + ". An EventType with the same name already exists.";
            throw new WrappedWebApplicationException(new WebApplicationException(errorMessage, Response.status(Response.Status.CONFLICT).entity(errorMessage).build()));
        }

        return eventTypeManagementDAO.createEventType(name, description, config);
    }

    public javax.json.JsonArray persistStatements(List<javax.json.JsonObject> statements) throws FailedToPersistStatementException {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (javax.json.JsonObject statement : statements) {
            StatementTO statementTO = persistStatement(statement);
            if (statementTO == null) {
                throw new FailedToPersistStatementException("Statement " + statement.getString("name") + " could not be persisted. Please check your configuration. Do you possibly have duplicate statement names?");
            }
            final String id = statementTO.getId();
            jsonArrayBuilder.add(id);
        }
        return jsonArrayBuilder.build();
    }

    public StatementTO persistStatement(javax.json.JsonObject statement) {
        final String name = statement.getString("name");
        final String query = statement.getString("query");
        final String comment = statement.getString("comment", null);
        return statementManagementDAO.createStatement(name, query, comment);
    }

    public void bulkRegisterWithEventProcessor(String virtualSensorId, String eventTypeIdsJson, String resultStatementId, String statementIdsJson, String resultEventTypeId, List<String> targets) throws EventProcessorWrapperException {
        List<String> eventTypeIds = readFromArrayJson(eventTypeIdsJson);
        List<String> statementIds = readFromArrayJson(statementIdsJson);

        JsonArray eventTypes = queryEventTypes(eventTypeIds);
        JsonArray statements = queryStatements(statementIds);
        JsonObject resultStatement = queryStatement(resultStatementId);
        JsonObject resultEventType = queryEventType(resultEventTypeId);
        JsonArray targetsArray = new JsonArray();
        if (targets != null) {
            targetsArray = new JsonArray(targets);
        }
        resultStatement.put("targets", targetsArray);

        eventProcessor.registerVirtualSensor(virtualSensorId, eventTypes, statements, resultStatement, resultEventType);
    }

    public void virtualSensorTargetsUpdateWithEventProcessor(String resultStatementName, String targets) throws EventProcessorWrapperException {
        eventProcessor.updateVirtualSensorTargets(resultStatementName, targets);
    }

    private List<String> readFromArrayJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            javax.json.JsonArray jsonArray = reader.readArray();
            List<String> result = new LinkedList<>();
            jsonArray
                    .stream()
                    .map(obj -> ((JsonString) obj).getString())
                    .forEachOrdered(result::add);
            return result;
        }
    }

    private JsonArray queryEventTypes(List<String> eventTypeIds) {
        List<JsonObject> result = eventTypeIds.stream()
                .map(this::queryEventType)
                .collect(Collectors.toList());
        return new JsonArray(result);
    }

    private JsonObject queryEventType(String eventTypeId) {
        EventTypeTO eventType = eventTypeManagementDAO.getById(eventTypeId);
        return new JsonObject(eventType.toJson().toString());
    }

    private JsonArray queryStatements(List<String> statementIds) {
        JsonArray result = new JsonArray();
        statementIds.stream().map(this::queryStatement).forEach(result::add);
        return result;
    }

    private JsonObject queryStatement(String statementId) {
        StatementTO statement = statementManagementDAO.getById(statementId);
        JsonObject json = new JsonObject();
        json.put("name", statement.getName())
                .put("query", statement.getQuery())
                .put("id", statement.getId());
        return json;
    }
}
