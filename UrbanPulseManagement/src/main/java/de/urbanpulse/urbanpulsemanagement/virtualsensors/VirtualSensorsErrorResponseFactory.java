package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsemanagement.services.*;
import java.util.Optional;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.core.Response;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class VirtualSensorsErrorResponseFactory {

    /**
     *
     * @param configuration the configuration to be checked
     * @return Optional.of(response) if an error was detected or
     * Optional.empty() if everything is fine
     */
    public Optional<Response> getErrorResponseForMissingElements(JsonObject configuration) {
        int httpStatus = AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
        Optional<Response> response = checkConfiguration(configuration, httpStatus);

        return response;
    }

    public Optional<Response> checkConfiguration(JsonObject configuration, int httpStatus) {
        Response response = null;

        if (!configuration.containsKey("statements")) {
            response = Response.status(httpStatus).entity("statements missing").build();
        } else if (!configuration.containsKey("resultEventType")) {
            response = Response.status(httpStatus).entity("resultEventType missing").build();
        } else if (!configuration.containsKey("description")) {
            response = Response.status(httpStatus).entity("description missing").build();
        } else if (!configuration.containsKey("category")) {
            response = Response.status(httpStatus).entity("category missing").build();
        } else if (!configuration.containsKey("eventTypes")) {
            response = Response.status(httpStatus).entity("eventtypes missing").build();
        } else if (configuration.containsKey("resultstatement")) {
            response = Response.status(httpStatus).entity("resultstatement not supported").build();
        }
        if (configuration.containsKey("targets") && configuration.get("targets").getValueType() != ValueType.ARRAY) {
            response = Response.status(httpStatus).entity("invalid targets array").build();
        }
        return Optional.ofNullable(response);
    }
}
