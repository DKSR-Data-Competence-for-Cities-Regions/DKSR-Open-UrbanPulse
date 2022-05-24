package de.urbanpulse.dist.jee.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_virtual_sensors")
@NamedQueries({
    @NamedQuery(name = "allVirtualSensors", query = "SELECT s FROM VirtualSensorEntity s"),
    @NamedQuery(name = "getByResultStatementName",
    query = "SELECT vs FROM VirtualSensorEntity vs, StatementEntity st WHERE vs.resultStatement = st AND st.name = :resultStatementName")
})
public class VirtualSensorEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.LAZY)
    private StatementEntity resultStatement;

    @OneToOne(fetch = FetchType.LAZY)
    private CategoryEntity category;

    @Lob
    private String description;

    @Lob
    private String statementIds;

    @Lob
    private String eventTypeIds;

    private String targets;

    @OneToOne
    private EventTypeEntity resultEventType;

    public String getTargets() {
        return targets;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public EventTypeEntity getResultEventType() {
        return resultEventType;
    }

    public void setResultEventType(EventTypeEntity resultEventType) {
        this.resultEventType = resultEventType;
    }

    public String getEventTypeIds() {
        return eventTypeIds;
    }

    public void setEventTypeIds(String eventTypeIds) {
        this.eventTypeIds = eventTypeIds;
    }

    public StatementEntity getResultStatement() {
        return resultStatement;
    }

    public void setResultStatement(StatementEntity resultStatement) {
        this.resultStatement = resultStatement;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public void setCategory(CategoryEntity category) {
        this.category = category;
    }

    public String getStatementIds() {
        return statementIds;
    }

    public void setStatementIds(String statementIds) {
        this.statementIds = statementIds;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
