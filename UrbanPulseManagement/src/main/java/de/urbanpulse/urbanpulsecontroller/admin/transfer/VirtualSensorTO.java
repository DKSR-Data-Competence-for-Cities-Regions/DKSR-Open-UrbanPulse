package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class VirtualSensorTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String description;
    private String categoryId;
    private String resultStatementId;
    private String resultEventTypeId;
    private String targets;

    public VirtualSensorTO() {
    }

    public VirtualSensorTO(VirtualSensorEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("VirtualSensorEntity has null id!");
        }
        this.id = entity.getId();
        this.description = entity.getDescription();
        this.categoryId = "" + entity.getCategory().getId();
        this.resultStatementId = "" + entity.getResultStatement().getId();
        this.targets = entity.getTargets() == null ? "[]" : entity.getTargets();
        Optional.ofNullable(entity.getResultEventType()).ifPresent((EventTypeEntity eventType)
                -> VirtualSensorTO.this.resultEventTypeId = "" + eventType.getId()
        );
    }

    public String getTargets() {
        return targets;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public String getResultEventTypeId() {
        return resultEventTypeId;
    }

    public void setResultEventTypeId(String resultEventTypeId) {
        this.resultEventTypeId = resultEventTypeId;
    }

    /**
     * @return SID, which is the numerical entity ID
     *
     */
    public String getSid() {
        return this.id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getResultStatementId() {
        return resultStatementId;
    }

    public void setResultStatementId(String resultStatementId) {
        this.resultStatementId = resultStatementId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.id);
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
        final VirtualSensorTO other = (VirtualSensorTO) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + this.id + " ]";
    }

    public JsonObject toJson() {
        String descriptionJsonString = "{}";
        if (!this.description.isEmpty()) {
            descriptionJsonString = this.description;
        }
        try (JsonReader reader = Json.createReader(new StringReader(descriptionJsonString))) {
            JsonObject descriptionObject = reader.readObject();
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("SID", getSid());
            builder.add("description", descriptionObject);
            builder.add("category", categoryId);
            builder.add("resultstatement", resultStatementId);
            builder.add("targets", getTargetsArray());
            if (resultEventTypeId == null) {
                builder.addNull("resultEventType");
            } else {
                builder.add("resultEventType", resultEventTypeId);
            }

            return builder.build();
        }

    }

    private JsonArray getTargetsArray() {
        String targetString = targets == null ? "[]" : targets;
        try (JsonReader reader = Json.createReader(new StringReader(targetString))) {
            return reader.readArray();
        }
    }
}
