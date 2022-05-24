package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.PermissionManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.PermissionsRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
public class PermissionsRestService extends AbstractRestService {

    @EJB
    private PermissionManagementDAO permissionManagementDAO;

    public Response getPermissions() {
        List<PermissionTO> permissions = permissionManagementDAO.getAll();
        if(permissions == null) {
            permissions = new ArrayList<>();
        }
        return Response.ok(permissions).build();
    }

    public Response getPermission(String id) {
        PermissionTO permission = permissionManagementDAO.getById(id);
        if (permission != null) {
            return Response.ok(permission).build();
        } else {
            return ErrorResponseFactory.notFound("permission with id [" + id + "] does not exist");
        }
    }

    public Response createPermission(PermissionTO permission, UriInfo context, PermissionsRestFacade facade) {
        if (permission.getId() != null && permissionManagementDAO.getById(permission.getId()) != null) {
            return ErrorResponseFactory.conflict("permission with this id already exists");
        }

        PermissionTO result = permissionManagementDAO.createPermission(permission);
        if(result != null) {
            URI location = getItemUri(context, facade, result.getId());
            return Response.created(location).build();
        } else {
            return Response.serverError().build();
        }

    }

    public Response updatePermission(String id, PermissionTO permission) {
        // Inject ID as it is not (or at least may not be) referenced in the request's body, else check
        if (permission.getId() == null) {
            permission.setId(id);
        } else if (!permission.getId().equals(id)) {
            return ErrorResponseFactory.unprocessibleEntity("given permission id does not match path parameter");
        }

       PermissionTO result = permissionManagementDAO.updatePermission(permission);
       if(result != null) {
            return Response.noContent().build();
        } else {
            return ErrorResponseFactory.notFound("permission with id [" + id + "] does not exist");
        }
    }

    public Response deletePermission(String id) {
        permissionManagementDAO.deleteById(id);
        return Response.noContent().build();
    }

}
