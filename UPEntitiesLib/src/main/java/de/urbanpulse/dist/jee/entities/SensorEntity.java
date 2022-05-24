package de.urbanpulse.dist.jee.entities;


import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
@Table(name = "up_sensors")
@NamedQueries({
    @NamedQuery(name = "allSensors", query = "SELECT s FROM SensorEntity s")})
public class SensorEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 2L;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "EVENTTYPE_ID", nullable = false)
    private EventTypeEntity eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONNECTOR_ID")
    private ConnectorEntity connector;

    @ManyToMany(mappedBy = "sensors", fetch = FetchType.LAZY)
    private List<CategoryEntity> categories;



    @Lob
    private String description;

    @Lob
    private String location;

    public SensorEntity() {
    }



    /**
     * @return the eventType
     */
    public EventTypeEntity getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(EventTypeEntity eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the connector
     */
    public ConnectorEntity getConnector() {
        return connector;
    }

    /**
     * @param connector the connector to set
     */
    public void setConnector(ConnectorEntity connector) {
        this.connector = connector;
    }

    /**
     * @return the categories
     */
    public List<CategoryEntity> getCategories() {
        return categories;
    }

    /**
     * @param categories the categories to set
     */
    public void setCategories(List<CategoryEntity> categories) {
        this.categories = categories;
    }

    public void addCategory(CategoryEntity category) {
        categories.add(category);
    }

    public Void removeCategory(CategoryEntity category) {
        categories.remove(category);
        return null;
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





}
