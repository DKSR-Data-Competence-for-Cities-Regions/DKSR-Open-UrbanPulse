package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsemanagement.services.StatementsRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;

/**
 * REST web service to configure event processor statements
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("statements")
@Api(tags = "statement")
public class StatementsRestFacade extends AbstractRestFacade {

    @EJB
    private StatementsRestService service;

    /**
     * @param id of the statement to get the update listener for.
     * @return wrapped JSON array of update listeners for this statement
     */
    @RequiresRoles(ADMIN)
    @GET
    @Path("/{id}/update-listeners")
    @Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @ApiOperation(value = "Retrieve all registered update listeners for statement with given ID", response = UpdateListenerTO.class,
            responseContainer = "List")
    public Response getUpdateListeners(@PathParam("id") String id) {
        return service.getUpdateListeners(id);
    }

    @RequiresRoles(ADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @ApiOperation(value = "Retrieve all registered statements", response = StatementTO.class, responseContainer = "List")
    public Response getStatements() {
        return service.getStatements();
    }

    @RequiresRoles(ADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @Path("/{id}")
    @ApiOperation(value = "Retrieve registered statement with given ID", response = StatementTO.class)
    public Response getStatement(@PathParam("id") String id) {
        return service.getStatement(id);
    }

    @RequiresRoles(ADMIN)
    @POST
    @Consumes(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @ApiOperation(value = "Register new statement")
    public Response createStatement(@ApiParam(required = true) StatementTO statement) {
        return service.createStatement(statement, context, this);
    }

    @RequiresRoles(ADMIN)
    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Remove registered statement with given ID. Will only work if every update listener for the given "
            + "statement has been removed first.")
    public Response deleteStatement(@PathParam("id") final String id) {
        return service.deleteStatement(id);
    }

    @RequiresRoles(ADMIN)
    @POST
    @Path("/{id}/update-listeners")
    @Consumes(MediaType.APPLICATION_JSON + "; charset=utf-8")
    @ApiOperation(value = "Register new update listener for statement with given ID and authentication information")
    public Response registerUpdateListener(@PathParam("id") final String statementId, @ApiParam(required = true,
            value = "UpdateListeners should contain an \"authJson\" object instead of the deprecated hmac key. The authJson object looks like this: {\"authMethod\": \"BASIC\", \"user\": \"foo\", \"password\": \"bar\"}") UpdateListenerTO updateListener) {
        return service.registerUpdateListener(statementId, updateListener, context, this.getClass());
    }

    @RequiresRoles(ADMIN)
    @GET
    @Produces("application/json" + "; charset=utf-8")
    @Path("/{statementId}/update-listeners/{listenerId}")
    @ApiOperation(value = "Retrieve registered update listener with given ID for statement with given ID",
            response = UpdateListenerTO.class)
    public Response getListener(@PathParam("statementId") String statementId, @PathParam("listenerId") String listenerId) {
        return service.getListener(statementId, listenerId);
    }

    @RequiresRoles(ADMIN)
    @DELETE
    @Path("/{statementId}/update-listeners/{listenerId}")
    @ApiOperation(value = "Remove registered update listener with given ID for statement with given ID")
    public Response removeListener(@PathParam("statementId") String statementId, @PathParam("listenerId") String listenerId) {
        return service.removeListener(statementId, listenerId);
    }

}
