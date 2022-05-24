package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class EventTypeExtendedTO {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Map<String,Object> description;
    private Map<String,Object> config;
    private List<String> sensors;

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

    public Map<String, Object> getDescription() {
        return description;
    }

    public void setDescription(Map<String, Object> description) {
        this.description = description;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String,Object> config) {
        this.config = config;
    }

    public List<String> getSensors() {
        return sensors;
    }

    public void setSensors(List<String> sensors) {
        this.sensors = sensors;
    }


}
