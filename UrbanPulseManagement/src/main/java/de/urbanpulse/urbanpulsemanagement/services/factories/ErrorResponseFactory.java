package de.urbanpulse.urbanpulsemanagement.services.factories;

import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ErrorResponseFactory {

    private final static String KEY_HTTP_ERROR_CODE = "httpErrorCode";
    private final static String KEY_ERROR_REASON = "reason";

    /**
     * Creates a response for the given status, containing a JsonObject with the given status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": {httpStatus},
     *   "reason": {message}
     * }
     *
     * @param httpStatus the response status
     * @param message the response message
     * @return A response for the given status, containing a JsonObject with the given status and the given message
     */
    public static Response fromStatus(Status httpStatus, String message) {
        message = checkMessageForNull(message);
        JsonObject responseBody = Json.createObjectBuilder()
                .add(KEY_HTTP_ERROR_CODE, httpStatus.getStatusCode())
                .add(KEY_ERROR_REASON, message)
                .build();
        return Response.status(httpStatus).entity(responseBody).build();
    }

    /**
     * Creates a response for the given status, containing a JsonObject with the given status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": {httpStatus},
     *   "reason": {message}
     * }
     *
     * @param httpStatus the response status
     * @param message the response message
     * @return A response for the given status, containing a JsonObject with the given status and the given message
     */
    public static Response fromStatus(int httpStatus, String message) {
        message = checkMessageForNull(message);
        JsonObject responseBody = Json.createObjectBuilder()
                .add(KEY_HTTP_ERROR_CODE, httpStatus)
                .add(KEY_ERROR_REASON, message)
                .build();
        return Response.status(httpStatus).entity(responseBody).build();
    }

    /**
     * Creates a 500 Internal Server Error response, containing a JsonObject with the status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": 500,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 500 Internal Server Error response, containing a JsonObject with the status and the given message
     */
    public static Response internalServerError(String message) {
        return ErrorResponseFactory.fromStatus(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Creates a 500 Internal Server Error response, containing a JsonObject with the status and a message
     * in the following schema:
     * {
     *   "httpErrorCode": 500,
     *   "reason": {message}
     * }
     * where message will be the message of the given exception
     *
     * @param ex the exception which the response message should be taken from
     * @return A 500 Internal Server Error response, containing a JsonObject with the status and the given message
     */
    public static Response internalServerErrorFromException(Exception ex) {
        return ErrorResponseFactory.fromStatus(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Creates a 400 Bad Request response, containing a JsonObject with the status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": 400,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 400 Bad Request response, containing a JsonObject with the status and the given message
     */
    public static Response badRequest(String message) {
        return ErrorResponseFactory.fromStatus(Response.Status.BAD_REQUEST, message);
    }

    /**
     * Creates a 403 Forbidden response
     * in the following schema:
     * {
     *   "httpErrorCode": 403,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 400 Bad Request response, containing a JsonObject with the status and the given message
     */
    public static Response forbidden(String message){
        return ErrorResponseFactory.fromStatus(Status.FORBIDDEN, message);
    }

    /**
     * Creates a 409 Conflict response, containing a JsonObject with the status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": 409,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 409 Conflict response, containing a JsonObject with the status and the given message
     */
    public static Response conflict(String message) {
        return ErrorResponseFactory.fromStatus(Response.Status.CONFLICT, message);
    }

    /**
     * Creates a 404 Not Found response, containing a JsonObject with the status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": 404,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 500 response, containing a JsonObject with the status and the given message
     */
    public static Response notFound(String message) {
        return ErrorResponseFactory.fromStatus(Response.Status.NOT_FOUND, message);
    }

    /**
     * Creates a 422 Unprocessable Entity response, containing a JsonObject with the status and the given message
     * in the following schema:
     * {
     *   "httpErrorCode": 422,
     *   "reason": {message}
     * }
     *
     * @param message the response message
     * @return A 422 Unprocessable Entity response, containing a JsonObject with the status and the given message
     */
    public static Response unprocessibleEntity(String message) {
        return ErrorResponseFactory.fromStatus(HTTP_STATUS_UNPROCESSABLE_ENTITY, message);
    }

    /**
     * Adding a null value to an json will fail, which might happen if e.g. ex.getMessage() returns null,
     * so we check that in advance.
     * @param message the message to check against null
     * @return the message or, if th message is null, the String "No message provided"
     */
    private static String checkMessageForNull(String message) {
        return (message == null) ? "No message provided" : message;
    }

}
