package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConnectorTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String description;
    private String key;
    private List<String> sensors;
    private String backchannelEndpoint;
    private String backchannelKey;

    public ConnectorTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to non-null values!)
     */
    public ConnectorTO(ConnectorEntity entity) {
        this.id = entity.getId();
        this.description = entity.getDescription();
        this.key = entity.getKey();
        this.backchannelKey = entity.getBackchannelKey();
        this.backchannelEndpoint = entity.getBackchannelEndpoint();

        this.sensors = new LinkedList<>();
        for (SensorEntity sensor : entity.getSensors()) {
            this.sensors.add(sensor.getId());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the secretkey
     */
    public String getKey() {
        return key;
    }

    /**
     * @param secretkey the secretkey to set
     */
    public void setKey(String secretkey) {
        this.key = secretkey;
    }

    /**
     * @return the sensors
     */
    public List<String> getSensors() {
        return sensors;
    }

    /**
     * @param sensors the sensors to set
     */
    public void setSensors(List<String> sensors) {
        this.sensors = sensors;
    }

    public String getBackchannelEndpoint() {
        return backchannelEndpoint;
    }

    public void setBackchannelEndpoint(String backchannelEndpoint) {
        this.backchannelEndpoint = backchannelEndpoint;
    }

    public String getBackchannelKey() {
        return backchannelKey;
    }

    public void setBackchannelKey(String backchannelKey) {
        this.backchannelKey = backchannelKey;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConnectorTO other = (ConnectorTO) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }

    public JsonObject toJson() {
        String descriptionJsonString = "{}";
        if (!this.description.isEmpty()) {
            descriptionJsonString = this.description;
        }
        try (JsonReader jsonReader = Json.createReader(new StringReader(descriptionJsonString))) {
            JsonObject descriptionObject = jsonReader.readObject();

            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("id", this.id);
            b.add("key", this.key);
            b.add("description", descriptionObject);

            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (String sensor : sensors) {
                arrayBuilder.add(sensor);
            }

            b.add("sensors", arrayBuilder.build());

            if (this.backchannelEndpoint == null) {
                b.addNull("backchannelEndpoint");
            } else {
                b.add("backchannelEndpoint", this.backchannelEndpoint);
            }

            if (this.backchannelKey == null) {
                b.addNull("backchannelKey");
            } else {
                b.add("backchannelKey", this.backchannelKey);
            }

            return b.build();
        }
    }
}
