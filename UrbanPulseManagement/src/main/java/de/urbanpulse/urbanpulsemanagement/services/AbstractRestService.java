package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.restfacades.dto.ScopesWithOperations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

/**
 * abstract baseclass for REST services
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractRestService {
    public static final int HTTP_STATUS_CONFLICT = 409;
    public static final int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;

    @Context
    protected UriInfo context;

    protected URI getItemUri(UriInfo context, AbstractRestFacade facade, String id) throws IllegalArgumentException, UriBuilderException {
        UriBuilder builder = context.getBaseUriBuilder();
        return builder.path(facade.getClass()).path(id).build();
    }

    /**
     * Creating the wild card strings which will be the permissions name
     *
     * @param sensorId   is the id of the sensor which should be part of the permission name
     * @param operations can be "read", "write", "delete", "*". Also part of the permission name
     * @param scopes     can be "livedata", "historicdata", "model", "permission", "*". Also part of the permission name
     * @return with the created wild card strings based on the scopes, operations, sensorId
     */
    protected List<String> createWildCardStrings(String sensorId, List<String> operations, List<String> scopes) {
        List<String> wildCards = new ArrayList<>();
        operations.forEach(operation ->
                scopes.forEach(scope -> {
                    String wildCard = "sensor:" + sensorId + ":" + scope + ":" + operation;
                    wildCards.add(wildCard);
                }));
        return wildCards;
    }

    protected Optional<PermissionTO> getPermissionBasedOnName(String permissionName, PermissionManagementDAO permissionDao) {
        return permissionDao.getFilteredBy("name", permissionName).stream()
                .findFirst();
    }

    protected boolean isValidPermissionBodyForSensor(ScopesWithOperations permissions) {
        return permissions.getOperation() != null && permissions.getScope() != null;
    }
}
