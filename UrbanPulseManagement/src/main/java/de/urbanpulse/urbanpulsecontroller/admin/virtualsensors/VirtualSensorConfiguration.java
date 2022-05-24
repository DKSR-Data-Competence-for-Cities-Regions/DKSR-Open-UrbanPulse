package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.json.JsonObject;

/**
 *
 * This POJO holds the configuration of a Virtual Sensor
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VirtualSensorConfiguration {

    private List<JsonObject> statments; // List of Statement JsonObjects (see Statement POST Service)
    private List<JsonObject> eventTypes; // List of EventType JsonObjects (see EventType POST Service)
    private List<String> targets;
    private JsonObject resultstatement; //Statement JsonObject (see Statement POST Service)
    private JsonObject resultEventType;
    private JsonObject description; // JsonObject

    public VirtualSensorConfiguration() {
        this.statments = new ArrayList<>();
        this.eventTypes = new ArrayList<>();
    }

    public JsonObject getResultEventType() {
        return resultEventType;
    }

    public void setResultEventType(JsonObject resultEventType) {
        this.resultEventType = resultEventType;
    }

    public List<JsonObject> getStatments() {
        return statments;
    }

    public void setStatments(List<JsonObject> statments) {
        this.statments = statments;
    }

    public List<JsonObject> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<JsonObject> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public JsonObject getResultstatement() {
        return resultstatement;
    }

    public void setResultstatement(JsonObject resultstatement) {
        this.resultstatement = resultstatement;
    }

    public JsonObject getDescription() {
        return description;
    }

    public void setDescription(JsonObject description) {
        this.description = description;
    }

    /**
     * Validates configuration consistency and throws an Exception if the values
     * are inconsistent
     *
     * @throws VirtualSensorConfigurationException
     */
    public void validate() throws VirtualSensorConfigurationException {
        // Validate no duplicate statement names
        Optional<String> dup = getStatments()
                .stream()
                .map(statment -> statment.getString("name"))
                .collect(Collectors.groupingBy(Function.identity()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .findAny();
        if (dup.isPresent()) {
            throw new VirtualSensorConfigurationException("Duplicate statement name: " + dup.get());
        }

        if (targets != null && !targets.isEmpty()) {
            Set<String> set = new HashSet<>(targets);
            if (set.size() != targets.size()) {
                throw new VirtualSensorConfigurationException("Duplicate targets!");
            }
        }
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public List<String> getTargets() {
        return targets;
    }

}
