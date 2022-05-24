package de.urbanpulse.dist.jee.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.Table;
import javax.persistence.NamedQuery;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_permissions")
@NamedQueries({
        @NamedQuery(name = "getPermissionByName", query = "SELECT p FROM PermissionEntity p where p.name=:permissionName"),
        @NamedQuery(name = "getRolePermissionsBySensorId", query = "SELECT p FROM PermissionEntity p inner join p.roles role where role.id=:roleId AND p.name LIKE CONCAT('%',:sensorId,'%')"),
        @NamedQuery(name = "getUserPermissionsBySensorId", query = "SELECT p FROM PermissionEntity p inner join p.users user where user.id=:userId AND p.name LIKE CONCAT('%',:sensorId,'%')")
})

public class PermissionEntity extends AbstractUUIDEntity {

    @ManyToMany(mappedBy = "permissions")
    private List<RoleEntity> roles;

    @ManyToMany(mappedBy = "permissions")
    private List<UserEntity> users;

    private String name;

    public PermissionEntity() {
        this.roles = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    public PermissionEntity(String id, String name) {
        setId(id);
        setName(name);
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public List<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles) {
        this.roles = roles;
    }

    public List<UserEntity> getUsers() {
        return users;
    }

    public void setUsers(List<UserEntity> users) {
        this.users = users;
    }

}
