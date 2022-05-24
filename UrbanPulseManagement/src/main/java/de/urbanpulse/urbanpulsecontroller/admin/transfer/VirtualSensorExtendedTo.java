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
public class VirtualSensorExtendedTo {

    private List<StatementTO> statements;
    private List<EventTypeExtendedTO> eventTypes;
    private EventTypeExtendedTO resultEventType;
    private Map<String,Object> description;
    private String category;
    private List<String> targets;

    public List<StatementTO> getStatements() {
        return statements;
    }

    public void setStatements(List<StatementTO> statements) {
        this.statements = statements;
    }

    public List<EventTypeExtendedTO> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<EventTypeExtendedTO> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public EventTypeExtendedTO getResultEventType() {
        return resultEventType;
    }

    public void setResultEventType(EventTypeExtendedTO resultEventType) {
        this.resultEventType = resultEventType;
    }

    public Map<String, Object> getDescription() {
        return description;
    }

    public void setDescription(Map<String, Object> description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }




}
