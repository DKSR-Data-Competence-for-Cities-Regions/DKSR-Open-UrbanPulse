package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import de.urbanpulse.dist.jee.entities.PermissionEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class PermissionTO implements Serializable {

    @JsonPropertyDescription("UUID - optional on POST and PUT requests; must match the path parameter if given in PUT request, must not exist yet if given in POST request")
    private String id;

    @NotNull
    private String name;

    public PermissionTO() {
    }

    public PermissionTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public PermissionTO(String name) {
        this.name = name;
    }

    public PermissionTO(PermissionEntity permissionEntity) {
        if(permissionEntity != null) {
        this.id = permissionEntity.getId();
        this.name = permissionEntity.getName();
        }
    }

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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PermissionTO other = (PermissionTO) obj;
        return Objects.equals(this.id, other.id);
    }

    public PermissionEntity toEntity() {
        PermissionEntity entity = new PermissionEntity();
        entity.setName(this.name);
        entity.setId(this.id);
        return entity;
    }


}
