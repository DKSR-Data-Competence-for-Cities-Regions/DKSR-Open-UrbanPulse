package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.dist.jee.entities.CategoryEntity;
import io.vertx.core.json.JsonObject;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CategoryWithChildrenTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;

    private String parentCategory;
    private List<CategoryWithChildrenTO> childCategories;
    private List<SensorTO> sensors;

    public CategoryWithChildrenTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to
     * non-null values!)
     */
    public CategoryWithChildrenTO(CategoryEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();

        this.parentCategory = null;
        CategoryEntity parentEntity = entity.getParentCategory();
        if (parentEntity != null) {
            this.parentCategory = entity.getParentCategory().getId();
        }

        this.childCategories = new LinkedList<>();
        if (entity.getChildCategories() != null) {
            Logger.getLogger(CategoryWithChildrenTO.class.getName()).log(Level.INFO, "getChildCategories --> not null");
            for (CategoryEntity child : entity.getChildCategories()) {
                CategoryWithChildrenTO childTO = new CategoryWithChildrenTO(child);
                childCategories.add(childTO);
            }
        } else {
            Logger.getLogger(CategoryWithChildrenTO.class.getName()).log(Level.INFO, "getChildCategories --> null");
        }

        this.sensors = new LinkedList<>();
        for (SensorEntity sensor : entity.getSensors()) {
            SensorTO sensorTO = new SensorTO(sensor);
            this.sensors.add(sensorTO);
        }
    }

    public JsonObject toJson() {
        JsonObject descriptionJson = this.description.isEmpty() ? new JsonObject() : new JsonObject(this.description);

        List<JsonObject> childCategoriesJsons = childCategories.parallelStream()
                .map(CategoryWithChildrenTO::toJson)
                .collect(Collectors.toList());

        List<JsonObject> sensorsJsons = sensors.parallelStream()
                .map(SensorTO::toJson)
                .collect(Collectors.toList());

        JsonObject resultObj = new JsonObject();
        resultObj.put("id", id);
        resultObj.put("name", name);
        resultObj.put("description", descriptionJson);
        resultObj.put("parentCategory", (parentCategory == null) ? "" : parentCategory);
        resultObj.put("childCategories", childCategoriesJsons);
        resultObj.put("sensors", sensorsJsons);
        return resultObj;
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

    public void setParentCategory(String parentCategory) {
        this.parentCategory = parentCategory;
    }

    public List<CategoryWithChildrenTO> getChildCategories() {
        return childCategories;
    }

    public void setChildCategories(List<CategoryWithChildrenTO> childCategories) {
        this.childCategories = childCategories;
    }

    /**
     * @return the sensors
     */
    public List<SensorTO> getSensors() {
        return sensors;
    }

    /**
     * @param sensors the sensors to set
     */
    public void setSensors(List<SensorTO> sensors) {
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

        final CategoryWithChildrenTO other = (CategoryWithChildrenTO) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }

}
