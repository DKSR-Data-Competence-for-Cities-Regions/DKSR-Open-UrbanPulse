package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
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
public class EventConfigValidatorTest {

    @InjectMocks
    private EventConfigValidator eventConfigValidator;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void emptyConfigIsInvalid() {
        String configStr = "{}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertTrue(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithoutSIDIsInvalid() {
        String configStr = "{ \"timestamp\":\"java.util.Date\"}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertTrue(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithoutTimestampIsInvalid() {

        String configStr = "{\"SID\": \"string\"}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertTrue(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithInvalidTimestampIsInvalid() {
        String configStr = "{ \"SID\": \"string\", \"timestamp\":\"string\"}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertTrue(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithInvalidSIDIsInvalid() {
        String configStr = "{ \"SID\": \"long\", \"timestamp\":\"java.util.Date\"}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertTrue(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithTimestampAndSIDIsValid() {
        String configStr = "{ \"SID\": \"string\", \"timestamp\":\"java.util.Date\"}";
        JsonObject config = Json.createReader(new StringReader(configStr)).readObject();
        assertFalse(eventConfigValidator.isInvalid(config));
    }

    @Test
    public void configWithMapIsValid() {
        String configStr = "{ \"SID\": \"string\", \"timestamp\":\"java.util.Date\", \"sensors\":\"java.util.Map\"}";
        JsonObject jsonObject = Json.createReader(new StringReader(configStr)).readObject();
        assertFalse(eventConfigValidator.isInvalid(jsonObject));
    }

    @Test
    public void configWithListIsValid() {
        String configStr = "{ \"SID\": \"string\", \"timestamp\":\"java.util.Date\", \"sensorsList\":\"java.util.List\"}";
        JsonObject jsonObject = Json.createReader(new StringReader(configStr)).readObject();
        assertFalse(eventConfigValidator.isInvalid(jsonObject));
    }
}
