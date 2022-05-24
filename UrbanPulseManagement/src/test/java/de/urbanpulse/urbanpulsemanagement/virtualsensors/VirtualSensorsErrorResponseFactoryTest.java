package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualSensorsErrorResponseFactoryTest {

    @InjectMocks
    private VirtualSensorsErrorResponseFactory factory;

    private JsonObject fullConfiguration;
    private JsonObject missingResultEventTypeConfiguration;

    @Before
    public void setUp() {
        fullConfiguration = Json.createObjectBuilder()
                .add("category", "13")
                .add("resultEventType", Json.createObjectBuilder().build())
                .add("description", Json.createObjectBuilder().build())
                .add("statements", Json.createArrayBuilder().build())
                .add("eventTypes", Json.createArrayBuilder().build())
                .build();

        missingResultEventTypeConfiguration = Json.createObjectBuilder()
                .add("category", "13")
                .add("description", Json.createObjectBuilder().build())
                .add("statements", Json.createArrayBuilder().build())
                .add("eventTypes", Json.createArrayBuilder().build())
                .build();
    }

    @Test
    public void getErrorResponseForMissingElements_returnsUnprocIfConfigurationAbsent() throws Exception {
        JsonObject json = Json.createObjectBuilder().build();

        Response response = factory.getErrorResponseForMissingElements(json).get();

        assertEquals(422, response.getStatus());
    }

    @Test
    public void getErrorResponseForMissingElements_returnsUnprocIfCustomArgsObjectIsPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("customargs", fullConfiguration)
                .build();
        Response response = factory.getErrorResponseForMissingElements(json).get();

        assertEquals(422, response.getStatus());
    }

    @Test
    public void getErrorResponseForMissingElements_returnsUnprocIfTemplateArgsObjectIsPresent() throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("templateargs", Json.createObjectBuilder())
                .build();
        Response response = factory.getErrorResponseForMissingElements(json).get();

        assertEquals(422, response.getStatus());
    }

    @Test
    public void getErrorResponseForMissingElements_returnsNullForFullConfiguration() throws Exception {
        final Optional<Response> errorResponse = factory.getErrorResponseForMissingElements(fullConfiguration);

        assertNotNull(errorResponse);
        assertFalse(errorResponse.isPresent());
    }


    @Test
    public void getErrorResponseForMissingElements_checkResultEventTypePresent() throws Exception {
        Optional<Response> errorResponse = factory.getErrorResponseForMissingElements(missingResultEventTypeConfiguration);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.isPresent());

        errorResponse = factory.getErrorResponseForMissingElements(fullConfiguration);

        assertNotNull(errorResponse);
        assertFalse(errorResponse.isPresent());
    }

}
