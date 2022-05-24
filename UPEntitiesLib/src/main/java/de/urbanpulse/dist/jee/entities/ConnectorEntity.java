package de.urbanpulse.dist.jee.entities;

import java.util.LinkedList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
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
@NamedQueries({
    @NamedQuery(name = "allConnectors", query = "SELECT c FROM ConnectorEntity c")
})
@Table(name = "up_connectors")
public class ConnectorEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 2L;

    @Lob
    private String description;

    /**
     * named "secretKey" because "key" is a reserved word in SQL
     */
    private String secretkey;

    @OneToMany(mappedBy = "connector")
    private List<SensorEntity> sensors;



    public ConnectorEntity() {
        sensors = new LinkedList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return secretkey;
    }

    public void setKey(String key) {
        this.secretkey = key;
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
