package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VirtualSensorConfigurationTest {

    private VirtualSensorConfiguration config;

    @Before
    public void setUp() {
        config = new VirtualSensorConfiguration();
    }

    @Test(expected = VirtualSensorConfigurationException.class)
    public void test_validate_duplicateStatementName_willThrowVirtualSensorConfigurationException() throws VirtualSensorConfigurationException {
        // "Statment" typo is intentional to keep in line with config.setStatments.
        JsonObject statment = Json.createObjectBuilder().add("name", "Horst").build();
        List<JsonObject> statments = new ArrayList<>();
        statments.add(statment);
        statments.add(statment);
        config.setStatments(statments);
        config.validate();
    }

    @Test
    public void test_validate_nonDuplicateStatementName_willNotThrowVirtualSensorConfigurationException() throws VirtualSensorConfigurationException {
        JsonObject statment = Json.createObjectBuilder().add("name", "Horst").build();
        List<JsonObject> statments = new ArrayList<>();
        statments.add(statment);
        config.setStatments(statments);
        config.validate();
    }

    @Test(expected = VirtualSensorConfigurationException.class)
    public void test_validate_duplicateTargets_willThrowVirtualSensorConfigurationException() throws VirtualSensorConfigurationException {
        List<String> targets = new ArrayList<>();
        targets.add("A Target");
        targets.add("A Target");
        config.setTargets(targets);
        config.validate();
    }

    @Test
    public void test_validate_duplicateTargets_willNotThrowVirtualSensorConfigurationException() throws VirtualSensorConfigurationException {
        List<String> targets = new ArrayList<>();
        targets.add("A Target");
        targets.add("Another Target");
        config.setTargets(targets);
        config.validate();
    }
}
