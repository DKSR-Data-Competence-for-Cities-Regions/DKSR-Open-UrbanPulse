package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EventTypeTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(EventTypeTO.class.getName());

    private String id;
    private String name;
    private String description;
    private String config;
    private List<String> sensors;

    public EventTypeTO() {
    }

    public JsonObject toJson() {
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("id", this.id);
        b.add("name", this.name);

        if (!isJSONValid(this.description)) {
            b.add("errorMessage", "Description is not a valid JSON");
            return b.build();
        }

        if (!isJSONValid(this.config)) {
            b.add("errorMessage", "Config is not a valid JSON");
            return b.build();
        }

        String descriptionJsonString = "{}";
        if (!this.description.isEmpty()) {
            descriptionJsonString = this.description;
        }
        try (JsonReader jsonReader = Json.createReader(new StringReader(descriptionJsonString))) {
            JsonObject descriptionObject = jsonReader.readObject();

            b.add("description", descriptionObject);

            String configJsonString = "{}";
            if (!this.config.isEmpty()) {
                configJsonString = this.config;
            }

            try (JsonReader configJsonReader = Json.createReader(new StringReader(configJsonString))) {
                JsonObject configObject = configJsonReader.readObject();
                b.add("config", configObject);

                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String sensor : sensors) {
                    arrayBuilder.add(sensor);
                }

                b.add("sensors", arrayBuilder.build());

                return b.build();
            }
        }
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to
     * non-null values!)
     */
    public EventTypeTO(EventTypeEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();

        this.sensors = new LinkedList<>();
        for (SensorEntity sensor : entity.getSensors()) {
            this.sensors.add(sensor.getId());
        }

        this.description = entity.getDescription();
        this.config = entity.getEventParameter();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public List<String> getSensors() {
        return sensors;
    }

    public void setSensors(List<String> sensors) {
        this.sensors = sensors;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (name != null ? name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventTypeTO)) {
            return false;
        }
        EventTypeTO other = (EventTypeTO) object;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ name=" + name + " ]";
    }

    public boolean isJSONValid(String test) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(test))) {
            jsonReader.read();
        } catch (JsonException ex) {
            LOG.log(Level.SEVERE, "Invalid Json: {0}", test);
            return false;
        }
        return true;
    }
}
