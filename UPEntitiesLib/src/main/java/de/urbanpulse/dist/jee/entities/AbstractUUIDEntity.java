package de.urbanpulse.dist.jee.entities;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@MappedSuperclass
public abstract class AbstractUUIDEntity implements Serializable {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public final  void setId(String id) {
        this.id = id;
    }

    @PrePersist
    protected void initEntity() {
        if (this.getId() == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[ id=" + getId() + " ]";
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AbstractUUIDEntity)) {
            return false;
        }
        AbstractUUIDEntity other = (AbstractUUIDEntity) obj;
        return getId().equals(other.getId());
    }
}
