package de.urbanpulse.urbanpulsecontroller.admin.entities.modules;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * holds setup JSON for UP inbound vert.x modules tailored to individual module
 * instances
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_inbound_setup")
public class InboundSetupEntity extends AbstractModuleSetupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InboundSetupEntity)) {
            return false;
        }
        InboundSetupEntity other = (InboundSetupEntity) object;
        return !((this.getId() == null && other.getId() != null) || (this.getId() != null && !this.getId().equals(other.getId())));
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != null ? getId().hashCode() : 0);
        return hash;
    }

}
