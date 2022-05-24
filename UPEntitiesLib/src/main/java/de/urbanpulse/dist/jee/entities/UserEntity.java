package de.urbanpulse.dist.jee.entities;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_users", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@NamedQueries({
    @NamedQuery(name = "userByName", query = "SELECT u FROM UserEntity u WHERE u.name=:name")})
public class UserEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 1L;

    @ManyToMany
    @JoinTable(name = "up_user_roles", joinColumns = {
        @JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {
                @JoinColumn(name = "role_id", referencedColumnName = "id")})
    private List<RoleEntity> roles;

    @ManyToMany
    @JoinTable(name = "up_user_permissions", joinColumns = {
        @JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {
                @JoinColumn(name = "permission_id", referencedColumnName = "id")})
    private List<PermissionEntity> permissions;

    private String name;
    /**
     * named "secretKey" because "key" is a reserved word in SQL
     */
    private String secretKey;
    private String passwordHash;

    public UserEntity() {
        this.roles = new ArrayList<>();
        this.permissions = new ArrayList<>();
    }

    public List<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles) {
        this.roles = roles;
    }

    public List<PermissionEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionEntity> permissions) {
        this.permissions = permissions;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return secretKey for HMAC auth
     */
    public String getKey() {
        return secretKey;
    }

    /**
     * @param secretKey for HMAC auth
     */
    public void setKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "de.urbanpulse.urbanpulsecontroller.usermanagement.UserEntity[ id=" + this.getId() + " ]";
    }

}
