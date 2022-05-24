package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 *
 * The VirtualSensorConfigurator creates the configuration and preconditions for
 * a Virtual Sensor based on a provided Category ID and Operation.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
public class VirtualSensorConfigurator {

    private static final String RESULT_STATEMENT_TEMPLATE = "select '<SID_PLACEHOLDER>' as SID, current_timestamp().toDate() as timestamp, <EVENT_TYPE_PARAMETERS> from <EVENT_TYPE>";

    private static final String KEY_CONFIG = "config";

    /**
     *
     * @param args Custom defined arguments passed to the REST API
     * @return A VirtualSensorConfiguration Object, that holds all information
     * {EventType definition and Statment definition} to create a Virtual Sensor
     * @throws VirtualSensorConfigurationException the virtual sensor could not
     * be configured.
     */
    public VirtualSensorConfiguration createVirtualSensorConfiguration(JsonObject args) throws VirtualSensorConfigurationException {
        VirtualSensorConfiguration configuration = new VirtualSensorConfiguration();
        configuration.setDescription(args.getJsonObject("description"));
        configuration.setEventTypes(jsonObjectListFromJsonArray(args.getJsonArray("eventTypes")));
        configuration.setStatments(jsonObjectListFromJsonArray(args.getJsonArray("statements")));
        if (args.containsKey("targets")) {
            configuration.setTargets(args.getJsonArray("targets").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.toList()));
        } else {
            configuration.setTargets(new ArrayList<>());
        }

        validateResultEventType(args.getJsonObject("resultEventType"));

        configuration.setResultstatement(createResultStatement(args.getJsonObject("resultEventType")));
        configuration.setResultEventType(args.getJsonObject("resultEventType"));
        return configuration;
    }

    protected void validateResultEventType(JsonObject resultEventType) throws VirtualSensorConfigurationException {

        boolean containsTimestamp = resultEventType.getJsonObject(KEY_CONFIG).containsKey("timestamp")
                && resultEventType.getJsonObject(KEY_CONFIG).getString("timestamp").equals("java.util.Date");
        boolean containsSID = resultEventType.getJsonObject(KEY_CONFIG).containsKey("SID")
                && resultEventType.getJsonObject(KEY_CONFIG).getString("SID").equals("string");

        if (!(containsSID && containsTimestamp)) {
            throw new VirtualSensorConfigurationException("Invalid ResultEventType. Check SID and timestamp");
        }
    }

    protected JsonObject createResultStatement(JsonObject resultEventType) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        resultEventType.getJsonObject(KEY_CONFIG).keySet().stream()
                .filter(s -> !"SID".equalsIgnoreCase(s) && !"timestamp".equalsIgnoreCase(s))
                .forEach(stringJoiner::add);
        String query = RESULT_STATEMENT_TEMPLATE
                .replace("<EVENT_TYPE>", resultEventType.getString("name"))
                .replace("<EVENT_TYPE_PARAMETERS>", stringJoiner.toString());
        String resultStatementName = resultEventType.getString("name") + "_VSResultStatement";
        return Json.createObjectBuilder()
                .add("name", resultStatementName)
                .add("query", query)
                .build();
    }

    /**
     *
     * @param jsonArray the json array that will be change to list of json
     * objects
     * @return a List of JsonObjects
     */
    private List<JsonObject> jsonObjectListFromJsonArray(JsonArray jsonArray) {
        List<JsonObject> list = new LinkedList<>();
        for (JsonValue value : jsonArray) {
            JsonObject objectValue = (JsonObject) value;
            list.add(objectValue);
        }
        return list;
    }
}
