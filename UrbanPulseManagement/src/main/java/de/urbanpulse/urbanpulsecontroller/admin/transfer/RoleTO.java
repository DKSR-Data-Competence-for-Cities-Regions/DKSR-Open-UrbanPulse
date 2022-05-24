package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class RoleTO implements Serializable {

    @JsonPropertyDescription("UUID - optional on POST and PUT requests; must match the path parameter if given in PUT request, must not exist yet if given in POST request")
    private String id;

    @NotNull
    private String name;

    private List<PermissionTO> permissions;

    public RoleTO() {}

    public RoleTO(String id) {
        this(id, "");
    }

    public RoleTO(String id, String name) {
        this.id = id;
        this.name = name;
        this.permissions = new ArrayList<>();
    }

    public RoleTO(String id, String name, List<PermissionTO> permissions) {
        this(id, name);
        this.permissions = permissions;
    }

    public RoleTO(RoleEntity role) {
        this.id = role.getId();
        this.name = role.getName();
        this.permissions = new ArrayList<>();
        role.getPermissions().stream().map(PermissionTO::new).forEach(permissions::add);
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

    public List<PermissionTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionTO> permissions) {
        this.permissions = permissions;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.id);
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
        final RoleTO other = (RoleTO) obj;
        return Objects.equals(this.id, other.id);
    }


    public RoleEntity toEntity() {
        RoleEntity entity = new RoleEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        List<PermissionEntity> permissionEntities = this.permissions.stream().map(PermissionTO::toEntity).collect(Collectors.toList());
        entity.setPermissions(permissionEntities);
        return entity;
    }

}

