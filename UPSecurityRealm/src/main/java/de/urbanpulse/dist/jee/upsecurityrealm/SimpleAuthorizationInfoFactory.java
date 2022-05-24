package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.shiro.authz.SimpleAuthorizationInfo;

/**
 * Creates a SimpleAuthorizationInfo based on the provided UserEntity
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SimpleAuthorizationInfoFactory {

     public static SimpleAuthorizationInfo createSimpleAuthorizationInfo(UserEntity subject) {
        List<RoleEntity> roleEntities = new ArrayList(subject.getRoles());

        List<PermissionEntity> permissions = new ArrayList(subject.getPermissions());

        HashSet<String> roles = new HashSet();
        roleEntities.forEach(roleEntity -> {
            roles.add(roleEntity.getName());
            permissions.addAll(roleEntity.getPermissions());
        });

         Set<String> permissionStringList = permissions.stream().map(p -> p.getName()).collect(Collectors.toSet());

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(permissionStringList);
        return authorizationInfo;
    }

    public static SimpleAuthorizationInfo createSimpleAuthorizationInfoFromClaims(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map)) {
            return null;
        }

        Object rolesList = ((Map<String, Object>) realmAccess).get("roles");
        if (!(rolesList instanceof List)) {
            return null;
        }

        Set<String> roles = ((List<String>) rolesList).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toSet());

        return new SimpleAuthorizationInfo(roles);
    }

}
