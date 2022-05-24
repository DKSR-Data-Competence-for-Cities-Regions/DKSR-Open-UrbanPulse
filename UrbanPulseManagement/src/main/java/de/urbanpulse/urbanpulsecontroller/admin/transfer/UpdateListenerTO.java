package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.UpdateListenerEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class UpdateListenerTO implements Serializable {

    private static final long serialVersionUID = -1484268398141878498L;

    private String id;
    private String statementId;
    private String target;
    private String key;
    private AuthJsonTO authJson;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public UpdateListenerTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to non-null values!)
     */
    public UpdateListenerTO(UpdateListenerEntity entity) {
        id = entity.getId();
        statementId = entity.getStatement().getId();
        target = entity.getTarget();
        key = entity.getKey();
        authJson = new AuthJsonTO(entity.getAuthJson());
    }

    public UpdateListenerTO(UpdateListenerTO other) {
        id = other.getId();
        statementId = other.getStatementId();
        target = other.getTarget();
        key = other.getKey();
        authJson = other.getAuthJson() == null ? null : new AuthJsonTO(other.getAuthJson());
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public AuthJsonTO getAuthJson() {
        return authJson;
    }

    public void setAuthJson(AuthJsonTO authJson) {
        this.authJson = authJson;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UpdateListenerTO)) {
            return false;
        }
        UpdateListenerTO other = (UpdateListenerTO) object;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }
}
