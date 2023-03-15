package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.dist.jee.upsecurityrealm.LoginToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPAuthMode;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.services.ConnectorsRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.*;
import static de.urbanpulse.urbanpulsemanagement.restfacades.ConnectorsRestFacade.ROOT_PATH;
import org.apache.shiro.SecurityUtils;

/**
 * REST web service for registering connectors and accessing their keys
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "connector")
@Path(ROOT_PATH)
public class ConnectorsRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "connectors";

    @EJB
    private ConnectorsRestService service;

    /**
     * retrieve all registered connectors
     *
     * @return connectors wrapped in JSON object
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER}, logical = Logical.OR)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve all registered connectors", response = ConnectorTO.class, responseContainer = "List")
    public Response getAllConnectors() {
        return service.getAllConnectors();

    }

    /**
     * retrieve a registered connector
     *
     * @param id connector ID
     * @return connector
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Path("/{id}")
    @Produces("application/json" + "; charset=utf-8")
    @ApiOperation(value = "retrieve a registered connector specified by its id", response = ConnectorTO.class)
    public Response getConnector(@PathParam("id") String id) {
        if (SecurityUtils.getSubject().getPrincipal() instanceof LoginToken) {
            if (!isRequestingConnectorAllowed(id)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Updated ID does not match connector id.").build();
            }
        }
        
        return service.getConnector(id);
    }

    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER}, logical = Logical.OR)
    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "delete a registered connector specified by its id")
    public Response deleteConnector(@PathParam("id") String id) {
        return service.deleteConnector(id);
    }

    /**
     * create a new connector registration
     *
     * @param connectorJson serialized connector object with description field
     * @return 201 created response with location header set appropriately
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER}, logical = Logical.OR)
    @POST
    @Consumes("application/json")
    @ApiOperation(value = "register a new connector")
    public Response createConnector(@ApiParam(required = true) String connectorJson) {
        return service.createConnector(connectorJson, context, this);
    }

    /**
     * update an existing connector
     *
     * @param id            ID string of the connector to update
     * @param connectorJson serialized connector object with description field
     * @return 204 NO CONTENT on success
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, CONNECTOR}, logical = Logical.OR)
    @PUT
    @Consumes("application/json")
    @Path("/{id}")
    @ApiOperation(value = "update a registered connector specified by its id", code = 204)
    public Response updateConnector(@PathParam("id") String id, @ApiParam(required = true) String connectorJson) {
        if (SecurityUtils.getSubject().getPrincipal() instanceof LoginToken) {
            if (!isRequestingConnectorAllowed(id)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Updated ID does not match connector id.").build();
            }
        }
        return service.updateConnector(id, connectorJson);
    }

    /**
     * retrieve all registered sensors for given connector
     *
     * @param connectorId category ID string
     * @return sensors wrapped in JSON object
     */
    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER, APP_USER, CONNECTOR}, logical = Logical.OR)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @Path("/{id}/sensors")
    @ApiOperation(
            value = "Retrieve all registered sensors for the connector with the given ID.",
            response = SensorTO.class,
            responseContainer = "List"
    )
    public Response getSensorsForConnector(@PathParam("id") String connectorId) {
        return service.getSensorsForConnector(connectorId);
    }
    
    protected boolean isRequestingConnectorAllowed(String id) {
        LoginToken token = (LoginToken) SecurityUtils.getSubject().getPrincipal();
        return (token.getAuthmode().equals(UPAuthMode.BASIC) || token.getAuthmode().equals(UPAuthMode.UP)) || 
                (token.getAuthmode().equals(UPAuthMode.UPCONNECTOR) && token.getSubjectId().equals(id));
    }

}
