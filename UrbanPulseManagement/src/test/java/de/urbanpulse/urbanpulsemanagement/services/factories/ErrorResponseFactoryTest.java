package de.urbanpulse.urbanpulsemanagement.services.factories;

import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ErrorResponseFactoryTest {

    private static final String TEST_MESSAGE = "indeed, that's a message!";

    private final static String KEY_HTTP_ERROR_CODE = "httpErrorCode";
    private final static String KEY_ERROR_REASON = "reason";

    @Test
    public void test_fromStatus_withResponeStatus() {
        Response response = ErrorResponseFactory.fromStatus(Response.Status.FORBIDDEN, TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(403, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_fromStatus_withInt() {
        Response response = ErrorResponseFactory.fromStatus(HTTP_STATUS_UNPROCESSABLE_ENTITY, TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(422, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_internalServerError_fromString() {
        Response response = ErrorResponseFactory.internalServerError(TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(500, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_internalServerError_fromExeption() {
        Exception ex = new Exception(TEST_MESSAGE);
        Response response = ErrorResponseFactory.internalServerErrorFromException(ex);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(500, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_badRequest() {
        Response response = ErrorResponseFactory.badRequest(TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(400, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_conflict() {
        Response response = ErrorResponseFactory.conflict(TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(409, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_notFound() {
        Response response = ErrorResponseFactory.notFound(TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(404, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_unprocessibleEntity() {
        Response response = ErrorResponseFactory.unprocessibleEntity(TEST_MESSAGE);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(422, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(TEST_MESSAGE, responseBody.getString(KEY_ERROR_REASON));
    }

    @Test
    public void test_fromStatus_withJsonEncodedMessage() {
        String jsonEncodedMessage = "\"httpStatusCode\":200,\"reason\":\"ab\"";
        Response response = ErrorResponseFactory.unprocessibleEntity(jsonEncodedMessage);
        JsonObject responseBody = (JsonObject)response.getEntity();
        assertEquals(422, responseBody.getInt(KEY_HTTP_ERROR_CODE));
        assertEquals(jsonEncodedMessage, responseBody.getString(KEY_ERROR_REASON));
    }

}
