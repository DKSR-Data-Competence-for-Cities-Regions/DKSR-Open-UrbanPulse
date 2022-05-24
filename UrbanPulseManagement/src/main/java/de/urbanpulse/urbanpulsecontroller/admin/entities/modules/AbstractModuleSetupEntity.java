package de.urbanpulse.urbanpulsecontroller.admin.entities.modules;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractModuleSetupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String setupJson;

    // use the UPModuleEntity's ID
    private String moduleId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSetupJson() {
        return setupJson;
    }

    public void setSetupJson(String setupJson) {
        this.setupJson = setupJson;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }




    @Override
    public String toString() {
        return this.getClass().getName() + "[ id=" + id + " ]";
    }
}
