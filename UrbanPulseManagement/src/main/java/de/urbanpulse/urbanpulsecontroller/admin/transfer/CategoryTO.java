package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.dist.jee.entities.CategoryEntity;
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
public class CategoryTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;

    private String parentCategory;
    private List<String> childCategories;
    private List<String> sensors;

    private List<String> metadata;

    public CategoryTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to non-null values!)
     */
    public CategoryTO(CategoryEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();

        this.description = entity.getDescription();

        this.parentCategory = null;
        if (entity.getParentCategory() != null) {
            this.parentCategory = entity.getParentCategory().getId();
        }

        this.childCategories = new LinkedList<>();
        if (entity.getChildCategories() != null) {
            for (CategoryEntity child : entity.getChildCategories()) {
                childCategories.add(child.getId());
            }
        }

        this.sensors = new LinkedList<>();
        for (SensorEntity sensor : entity.getSensors()) {
            this.sensors.add(sensor.getId());
        }
    }

    public JsonObject toJson() {
        String descriptionJsonString = "{}";
        if (!this.description.isEmpty()) {
            descriptionJsonString = this.description;
        }
        try (JsonReader jsonReader = Json.createReader(new StringReader(descriptionJsonString))) {
            JsonObject descriptionObject = jsonReader.readObject();

            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

            JsonArrayBuilder childrenBuilder = Json.createArrayBuilder();
            for (String child : childCategories) {
                childrenBuilder.add(child);
            }

            JsonArrayBuilder sensorsBuilder = Json.createArrayBuilder();
            for (String sensor : sensors) {
                sensorsBuilder.add(sensor);
            }

            objectBuilder.add("id", id);
            objectBuilder.add("name", name);
            objectBuilder.add("description", descriptionObject);
            if (null == parentCategory) {
                objectBuilder.add("parentCategory", "");
            } else {
                objectBuilder.add("parentCategory", parentCategory);
            }
            objectBuilder.add("childCategories", childrenBuilder);
            objectBuilder.add("sensors", sensorsBuilder);
            return objectBuilder.build();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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

    public String getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(String parentCategoryName) {
        this.parentCategory = parentCategoryName;
    }

    public List<String> getChildCategories() {
        return childCategories;
    }

    public void setChildCategories(List<String> childCategories) {
        this.childCategories = childCategories;
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Objects.hashCode(this.id);
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

        final CategoryTO other = (CategoryTO) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<String> metadata) {
        this.metadata = metadata;
    }
}
