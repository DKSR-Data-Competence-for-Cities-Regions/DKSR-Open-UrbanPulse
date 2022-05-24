package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.dist.jee.entities.CategoryEntity;
import io.vertx.core.json.JsonObject;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SensorTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String eventType;
    private String senderid;
    private List<String> categories;
    private String description;
    private String location;

    public SensorTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to non-null values!)
     */
    public SensorTO(SensorEntity entity) {
        this.id = entity.getId();
        this.description = entity.getDescription();
        this.senderid = entity.getConnector().getId();

        this.categories = new LinkedList<>();
        for (CategoryEntity category : entity.getCategories()) {
            this.categories.add(category.getId());
        }

        this.eventType = entity.getEventType().getId();

        this.location = entity.getLocation();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the eventType as IDs
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType IDs to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the senderid (usually identifies a connector)
     */
    public String getSenderid() {
        return senderid;
    }

    /**
     * @param senderid the senderid to set (usually identifies a connector)
     */
    public void setSenderid(String senderid) {
        this.senderid = senderid;
    }

    /**
     * @return the category IDs
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * @param categories the category IDs to set
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
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
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
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
        final SensorTO other = (SensorTO) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }

    public JsonObject toJson() {
        JsonObject descriptionJson = this.description.isEmpty() ? new JsonObject() : new JsonObject(this.description);
        JsonObject locationJson = this.location.isEmpty() ? new JsonObject() : new JsonObject(this.location);

        JsonObject job = new JsonObject();
        job.put("id", this.id);
        job.put("description", descriptionJson);
        job.put("location", locationJson);
        job.put("senderid", this.senderid);
        job.put("categories", this.categories);
        job.put("eventtype", this.eventType);

        return job;
    }

}
