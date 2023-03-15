package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.urbanpulsecontroller.admin.EventTypeManagementDAO;
import de.urbanpulse.urbanpulsemanagement.restfacades.SchemaRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.helper.SupportedJsonSchemaTypes;
import de.urbanpulse.urbanpulsemanagement.services.helper.SupportedUPEventParameterTypes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class SchemaService {

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String OBJECT = "object";
    public static final String DESCRIPTION = "description";

    private final Map<String, JsonObject> eventTypeToJsonSchema = new HashMap<>();

    @EJB
    EventTypeManagementDAO dao;

    @PostConstruct
    protected void init() {
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_DATE, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_STRING).put("format", "date-time"));
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_DOUBLE, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_NUMBER));
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_LONG, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_INTEGER));
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_MAP, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_OBJECT));
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_LIST, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_ARRAY));
        eventTypeToJsonSchema.put(SupportedUPEventParameterTypes.TYPE_STRING, new JsonObject().put(TYPE, SupportedJsonSchemaTypes.TYPE_STRING));
    }

    public Response getEventTypeAsJsonSchema(String id, UriInfo context) {
        Optional<EventTypeEntity> result = Optional.ofNullable(dao.queryById(id));
        if (!result.isPresent()) {
            return ErrorResponseFactory.notFound("schema with ID[" + id + "] not found.");
        } else {
            JsonObject jsonSchema = translateEventTypeToJsonSchema(context, result.get());
            return Response.ok(jsonSchema.encodePrettily()).build();
        }

    }

    public Response getAllEventTypesAsJsonSchema(UriInfo context) {
        List<EventTypeEntity> eventTypes = dao.queryAll();
        List<JsonObject> jsonSchemas = eventTypes.stream().map(et -> translateEventTypeToJsonSchema(context, et)).collect(Collectors.toList());
        JsonObject response = new JsonObject().put("schemas", new JsonArray(jsonSchemas));
        return Response.ok(response.encodePrettily()).build();
    }

    /**
     * Translates an event type entity into json schema
     * @param context current request context
     * @param eventType the entity
     * @return json schema json object
     */
    private JsonObject translateEventTypeToJsonSchema(UriInfo context, EventTypeEntity eventType) {

        JsonObject upDescription = new JsonObject(eventType.getDescription());
        JsonObject upProperties = new JsonObject(eventType.getEventParameter());
        JsonObject schemaProperties = new JsonObject();
        JsonArray required = new JsonArray();
        JsonArray primaryKey = new JsonArray();

        upProperties.forEach(property -> {
            String description = upDescription.getString(property.getKey()) != null ? upDescription.getString(property.getKey()) : "";
            schemaProperties.put(property.getKey(), 
                    translateUpTypeToJsonSchemaType(property.getValue())
                   .put(DESCRIPTION, description));
        });

        return new JsonObject()
                .put("$id", buildSchemaId(context, eventType.getId()))
                .put("$schema", "http://json-schema.org/draft-07/schema#")
                .put("title", eventType.getName())
                .put(TYPE, OBJECT)
                .put("properties", schemaProperties)
                .put("required", required)
                .put("primaryKey", primaryKey);
    }

    private JsonObject translateUpTypeToJsonSchemaType(Object value) {
        String upType = (String) value;
        return eventTypeToJsonSchema.getOrDefault(upType, new JsonObject().put(TYPE, upType)).copy();
    }

    public Response getEventTypeAsJsonSchemaByName(String schemaName, UriInfo context) {
        List<EventTypeEntity> eventTypes = dao.queryFilteredBy("name", schemaName);
        if (eventTypes.isEmpty()) {
            return ErrorResponseFactory.notFound("schema with name [" + schemaName + "] not found.");
        } else {
            JsonObject response = new JsonObject().put("schemas", new JsonArray()
                    .add(translateEventTypeToJsonSchema(context, eventTypes.get(0))));
            return Response.ok(response.encodePrettily()).build();
        }
    }

    private String buildSchemaId(UriInfo context, String id) {
        UriBuilder builder = context.getBaseUriBuilder().path(SchemaRestFacade.class).path(id);
        return builder.build().toString();
    }
}
