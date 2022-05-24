package de.urbanpulse.dist.jee.entities;

import java.util.LinkedList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@NamedQueries({
    @NamedQuery(name = "allEventTypes", query = "SELECT t FROM EventTypeEntity t")
})
@Table(name = "up_event_types", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Entity
public class EventTypeEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 2L;

    private String name;

    // Aside from the special case varchar(max) which may or may not be a LOB type,
    // 8000 is the max varchar size in MS SQL Server
    @Column(length = 8000)
    private String description;
    @Column(length = 8000)
    private String eventParameter;

    @OneToMany(mappedBy = "eventType")
    private List<SensorEntity> sensors;

    public EventTypeEntity() {
        sensors = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return description JSON
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description description JSON
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the eventParameter
     */
    public String getEventParameter() {
        return eventParameter;
    }

    /**
     * @param eventParameter the eventParameter to set
     */
    public void setEventParameter(String eventParameter) {
        this.eventParameter = eventParameter;
    }

    /**
     * @return the sensors
     */
    public List<SensorEntity> getSensors() {
        return sensors;
    }

    public Void removeSensor(SensorEntity sensor) {
        this.sensors.remove(sensor);
        return null;
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
}
