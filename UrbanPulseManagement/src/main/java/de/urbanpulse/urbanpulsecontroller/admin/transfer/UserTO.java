package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class UserTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonPropertyDescription("User name - optional on PUT, but not on POST")
    private String name;

    @JsonPropertyDescription("UUID - optional on POST and PUT requests; must match the path parameter if given in PUT request, must not exist yet if given in POST request")
    private String id;

    @JsonPropertyDescription("Password - write only (will not be returned in GET requests)")
    private String password;

    @JsonPropertyDescription("SecretKey - hidden field")
    private String secretKey;

    @JsonPropertyDescription("User roles as full JSON objects (including ID and name)")
    private List<RoleTO> roles;

    @JsonPropertyDescription("User permissions in addition to the ones added by the user's roles; full JSON objects (including ID and name)")
    private List<PermissionTO> permissions;

    public UserTO() {
    }

    public UserTO(String name, String password, String secretKey, List<RoleTO> roles, List<PermissionTO> permissions) {
        this.name = name;
        this.password = password;
        this.roles = roles;
        this.permissions = permissions;
        this.secretKey = secretKey;
    }

    public UserTO(String id, String name, String password, String secretKey, List<RoleTO> roles, List<PermissionTO> permissions) {
        this(name, password, secretKey, roles, permissions);
        this.id = id;
    }

    public UserTO(UserEntity userEntity) {
        this(userEntity.getId(), userEntity.getName(), null, null, mapRoles(userEntity.getRoles()), mapPermissions(userEntity.getPermissions()));
    }

    private static List<RoleTO> mapRoles(List<RoleEntity> roleEntities) {
        if (roleEntities == null) {
            return Collections.emptyList();
        }
        return roleEntities.stream().map(RoleTO::new).collect(Collectors.toList());
    }

    private static List<PermissionTO> mapPermissions(List<PermissionEntity> permissionEntities) {
        if (permissionEntities == null) {
            return Collections.emptyList();
        }
        return permissionEntities.stream().map(PermissionTO::new).collect(Collectors.toList());
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<RoleTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleTO> roles) {
        this.roles = roles;
    }

    public List<PermissionTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionTO> permissions) {
        this.permissions = permissions;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (name != null ? name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UserTO)) {
            return false;
        }
        UserTO other = (UserTO) object;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ name=" + name + " ]";
    }
}
