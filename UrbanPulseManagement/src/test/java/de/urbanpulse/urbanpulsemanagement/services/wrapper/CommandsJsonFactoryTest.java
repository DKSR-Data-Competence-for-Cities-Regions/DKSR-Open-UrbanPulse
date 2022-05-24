package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import org.junit.Test;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Before;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CommandsJsonFactoryTest {

    private static final String virtualSensorId = "virtualSensorId";

    private CommandsJsonFactory commandsJsonFactory;

    @Before
    public void setUp() {
        commandsJsonFactory = new CommandsJsonFactory();
    }

    @Test
    public void test_createRegisterVirtualSensorCommands_includesRelevantData() {
        JsonObject eventType = new JsonObject().put("id", "the-event-type-id");
        JsonArray eventTypes = new JsonArray().add(eventType);
        JsonObject statement = new JsonObject().put("name", "the-statement-name");
        JsonArray statements = new JsonArray().add(statement);
        JsonObject resultStatement = new JsonObject().put("id", "the-result-statement-id");
        JsonObject resultEventType = new JsonObject().put("name", "the-result-event-type-name");

        JsonObject result = commandsJsonFactory.createRegisterVirtualSensorCommands(
                virtualSensorId,
                eventTypes,
                statements,
                resultStatement,
                resultEventType);
        JsonArray moduleTypeCommands = result.getJsonArray("moduleTypeCommands");
        assertEquals(1, moduleTypeCommands.size());
        JsonObject command = moduleTypeCommands.getJsonObject(0);
        JsonObject args = command.getJsonObject("args");

        assertEquals(5, args.size());
        assertEquals(virtualSensorId, args.getString("virtualSensorId"));
        assertEquals(eventTypes, args.getJsonArray("eventTypes"));
        assertEquals(statements, args.getJsonArray("statements"));
        assertEquals(resultStatement, args.getJsonObject("resultStatement"));
        assertEquals(resultEventType, args.getJsonObject("resultEventType"));
    }

    @Test
    public void test_createUnregisterVirtualSensorCommands_onlyIncludesVSId() {
        JsonObject result = commandsJsonFactory.createUnregisterVirtualSensorCommands(virtualSensorId);
        JsonArray moduleTypeCommands = result.getJsonArray("moduleTypeCommands");
        assertEquals(1, moduleTypeCommands.size());
        JsonObject command = moduleTypeCommands.getJsonObject(0);
        JsonObject args = command.getJsonObject("args");

        assertEquals(1, args.size());
        assertEquals(virtualSensorId, args.getString("virtualSensorId"));
    }
}
