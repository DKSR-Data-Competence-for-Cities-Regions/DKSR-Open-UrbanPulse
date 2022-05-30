package de.urbanpulse.urbanpulsecontroller.admin.entities.modules;

import de.urbanpulse.util.status.UPModuleState;
import java.io.Serializable;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.validation.constraints.NotNull;

/**
 * UP vert.x module registration
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_modules")
public class UPModuleEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    private String moduleType;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastHeartbeat;


    @Enumerated(EnumType.STRING)
    private UPModuleState moduleState;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public UPModuleState getModuleState() {
        return moduleState;
    }

    public void setModuleState(UPModuleState moduleState) {
        this.moduleState = moduleState;
    }

    public Date getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Date lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("id", id).add("moduleType", moduleType).build();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UPModuleEntity)) {
            return false;
        }
        UPModuleEntity other = (UPModuleEntity) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[ id=" + id + " ]";
    }

}
