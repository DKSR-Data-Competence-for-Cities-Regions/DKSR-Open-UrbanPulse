package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class ResponseDefFactory {

    static final String ERROR_PERSISTENCE_NOT_AVAILABLE = "Persistence not available.";
    static final String ERROR_BAD_REQUEST = "Bad request.";
    static final String ERROR_INTERNAL_SEVER_ERROR = "Internal server error.";

    private static final String FIELD_BODY = "body";
    private static final String FIELD_STATUS_CODE = "statusCode";
    private static final String FIELD_CONTENT_TYPE = "contentType";

    private static final String MIME_TYPE_TEXTHTML = "text/html";

    JsonObject createSidMissingResponseObject(Map<String, String> sensorInformation) {
        String value = null;
        String key = null;

        StringBuilder message = new StringBuilder("<h1>DATA SOURCE IS MISSING</h1>");
        message.append("<h3>Valid Data Sources are: </h3>");
        message.append("<ul>");
        for (String fieldName : sensorInformation.keySet()) {
            value = sensorInformation.get(fieldName);
            message.append("<li>").append(value).append(" has SID: ").append(fieldName).append("</li><br>");
            key = fieldName;
        }
        message.append("</ul>");
        message.append("<h3>Please query data by forming the link as follows: </h3>");
        message.append(
                "/UrbanPulseData/historic/sensordata?since=<strong>START_TIME</strong>&until=<strong>END_TIME</strong>&sid=<strong>SID</strong> <br><br>"
        );

        message.append("<strong>WHERE</strong><ul>");
        message.append("<li><strong>START_TIME</strong> format: yyyy-MM-ddThh:mm:ss.SSSZ e.g. 2016-05-01T00:10:40.180Z </li><br>");
        message.append("<li><strong>END_TIME</strong> format: yyyy-MM-ddThh:mm:ss.SSSZ e.g. 2016-05-01T00:10:41.180Z </li><br>");

        message.append("<li><strong>SID</strong> e.g. ").append(key).append(" for ").append(value).append(" </li><br>");
        message.append("</ul>");

        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, message);
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 400);
        return responseDef;
    }

    JsonObject createBadRequestResponseObject(String message) {
        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, "<h1>" + ERROR_BAD_REQUEST + " " +  (message == null ? "" : message) + "</h1>");
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 400);
        return responseDef;
    }

    JsonObject createInternalServerErrorResponseObject(String message) {
        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, "<h1>" + ERROR_INTERNAL_SEVER_ERROR + " " +  (message == null ? "" : message) + "</h1>");
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 500);
        return responseDef;
    }

    JsonObject createNoPermissionForSidResponseObject(String message) {
        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, message);
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 403);
        return responseDef;
    }

    JsonObject createPersistenceNotAvailableResponseObject() {
        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, "<h1>" + ERROR_PERSISTENCE_NOT_AVAILABLE + "</h1>");
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 500);
        return responseDef;
    }

    JsonObject createNotFoundResponseObject(String message) {
        JsonObject responseDef = new JsonObject();
        responseDef.put(FIELD_BODY, message);
        responseDef.put(FIELD_CONTENT_TYPE, MIME_TYPE_TEXTHTML);
        responseDef.put(FIELD_STATUS_CODE, 404);
        return responseDef;
    }
}
