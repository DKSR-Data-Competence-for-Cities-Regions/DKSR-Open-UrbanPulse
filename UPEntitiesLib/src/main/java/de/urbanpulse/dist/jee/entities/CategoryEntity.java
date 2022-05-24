package de.urbanpulse.dist.jee.entities;


import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_categories")
@NamedQueries({
    @NamedQuery(name = CategoryEntity.ALL_CATEGORIES_QUERY, query = "SELECT c FROM CategoryEntity c"),
    @NamedQuery(name = CategoryEntity.ALL_ROOT_CATEGORIES_QUERY,
            query = "SELECT c FROM CategoryEntity c WHERE c.parentCategory IS NULL"),
    @NamedQuery(name = CategoryEntity.CATEGORY_BY_NAME_QUERY,
            query = "SELECT c FROM CategoryEntity c WHERE c.name=:categoryName")})
public class CategoryEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 2L;

    public static final String ALL_CATEGORIES_QUERY = "allCategories";
    public static final String ALL_ROOT_CATEGORIES_QUERY = "allRootCategories";
    public static final String CATEGORY_BY_NAME_QUERY = "categoryByName";

    private String name;

    @Lob
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private CategoryEntity parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    private List<CategoryEntity> childCategories;

    @ManyToMany
    @JoinTable(name = "up_categoriesProjection", joinColumns = {
        @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "ID")},
            inverseJoinColumns = {
                @JoinColumn(name = "SENSOR_ID", referencedColumnName = "ID")})
    private List<SensorEntity> sensors;



    public CategoryEntity() {
        parentCategory = null;
        childCategories = new LinkedList<>();
        sensors = new LinkedList<>();

    }



    public String getName() {
        return name;
    }

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

    public CategoryEntity getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(CategoryEntity parentCategory) {
        this.parentCategory = parentCategory;
    }

    public List<CategoryEntity> getChildCategories() {
        return childCategories;
    }

    public void setChildCategories(List<CategoryEntity> childCategories) {
        this.childCategories = childCategories;
    }

    public void addChild(CategoryEntity child) {
        this.childCategories.add(child);
    }

    public void removeChild(CategoryEntity child) {
        this.childCategories.remove(child);
    }

    /**
     * @return the sensors
     */
    public List<SensorEntity> getSensors() {
        return sensors;
    }

    /**
     * @param sensors the sensors to set
     */
    public void setSensors(List<SensorEntity> sensors) {
        this.sensors = sensors;
    }

    public void addSensor(SensorEntity sensor) {
        this.sensors.add(sensor);
    }

    public Void removeSensor(SensorEntity sensor) {
        this.sensors.remove(sensor);
        return null;
    }

}
