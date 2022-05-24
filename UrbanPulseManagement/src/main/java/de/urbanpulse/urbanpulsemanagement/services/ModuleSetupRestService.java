package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.ModuleUpdateManager;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.SetupMasterConnector;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.CommandResultVerifier;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import io.vertx.core.json.JsonObject;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ModuleSetupRestService {

    @Inject
    private ModuleUpdateManager moduleUpdateManager;

    @Inject
    private SetupMasterConnector setupMasterConnector;

    @Inject
    private UPModuleDAO moduleDAO;

    public Response resetAllModules() {
        setupMasterConnector.reset();
        return Response.noContent().build();
    }

    public Response getRegistrations() {
        List<UPModuleEntity> entities = moduleDAO.queryAll();
        return buildRegistrationsResponse(entities);
    }

    public Response getRegistrations(String moduleType) {
        try {
            UPModuleType.valueOf(moduleType);
        } catch (RuntimeException e) {
            return ErrorResponseFactory.badRequest("invalid module type: " + moduleType);
        }

        List<UPModuleEntity> entities = moduleDAO.queryFilteredBy("moduleType", moduleType);
        return buildRegistrationsResponse(entities);
    }

    private Response buildRegistrationsResponse(List<UPModuleEntity> entities) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (UPModuleEntity entity : entities) {
            arrayBuilder.add(entity.toJsonObject());
        }

        builder.add("registrations", arrayBuilder);

        final String jsonString = builder.build().toString();
        return Response.ok(jsonString).build();
    }

    public Response resetModuleConnection(String moduleId) {
        String commandString = "{"
                + "\"method\" : \"resetConnection\","
                + "\"args\": {}"
                + "}";
        Response resp = sendModuleCommand(moduleId, commandString);
        setupMasterConnector.reset(moduleId);
        return resp;
    }

    public Response sendModuleCommand(String moduleId, String commandString) {
        JsonObject command = new JsonObject(commandString);
        CommandResultVerifier verifier = (JsonObject obj) -> {
            return true;
        };
        try {
            JsonObject commandResult = moduleUpdateManager.runSingleInstanceReturnCommand(moduleId, command, verifier);
            if (commandResult != null) {
                return Response.ok(commandResult.encodePrettily()).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Execution of command failed").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Execution of command failed").build();
        }
    }

    public Response exitModule(String moduleId) {
        String commandString = "{"
                + "\"method\" : \"exitProcess\","
                + "\"args\": {}"
                + "}";
        Response resp = sendModuleCommand(moduleId, commandString);
        setupMasterConnector.reset(moduleId);
        return resp;
    }
}
