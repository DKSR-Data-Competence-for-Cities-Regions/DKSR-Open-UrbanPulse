package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import de.urbanpulse.urbanpulsemanagement.services.ModuleSetupRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("moduleSetup")
@Api(tags = {"internal", "module setup", "clustering"})
public class ModuleSetupRestFacade {

    @EJB
    private ModuleSetupRestService service;

    /**
     * clear all module transactions / connections / registrations, then broadcast on the "module_reset" vertx address,
     * every module registers a handler on this address and resets its own state
     *
     * @param req the request from the remote host to reset all the modules
     * @return 204 NO CONTENT
     */
    @POST
    @Path("reset")
    @ApiOperation(
            value = "clear all module transactions / connections / registrations"
    )
    @RequiresRoles(ADMIN)
    public Response resetAllModules(@Context HttpServletRequest req) {
        Logger.getLogger(ModuleSetupRestFacade.class.getName()).log(Level.INFO, "Reset of all modules requested from {0}", new Object[] { req.getRemoteHost() });
        return service.resetAllModules();
    }

    /**
     * get all registered module instances
     *
     * @return JSON object containing an array "registrations" with objects containing (module-)"id" and "moduleType"
     * each
     */
    @GET
    @Path("registrations")
    @Produces("application/json")
    @ApiOperation(
            value = "get all registered module instances",
            response = UPModuleEntity.class,
            responseContainer = "List"
    )
    @RequiresRoles(ADMIN)
    public Response getRegistrations() {
        return service.getRegistrations();
    }

    /**
     * get all registered module instances of a certain module type
     *
     * @param moduleType name of any {@link UPModuleType} value
     * @return JSON object containing an array "registrations" with objects containing (module-)"id" and "moduleType"
     * each / 400 BAD REQUEST for unsupported module type
     */
    @GET
    @Path("registrations/{moduleType}")
    @Produces("application/json")
    @ApiOperation(
            value = "get all registered module instances of a certain module type",
            response = UPModuleEntity.class,
            responseContainer = "List",
            nickname = "getRegistrationsForModuleType"
    )
    @RequiresRoles(ADMIN)
    public Response getRegistrations(@PathParam("moduleType") String moduleType) {
        return service.getRegistrations(moduleType);
    }

    @POST
    @Consumes("application/json")
    @Path("resetModule/{id}")
    @ApiOperation(
            value = "clear transactions / connections / registrations for module with given ID"
    )
    @RequiresRoles(ADMIN)
    public Response resetModule(@PathParam("id") String id, @Context HttpServletRequest req) {
        Logger.getLogger(ModuleSetupRestFacade.class.getName()).log(Level.INFO, "Reset of module {0} requested from {1}", new Object[] { id, req.getRemoteHost() });
        return service.resetModuleConnection(id);
    }

    @POST
    @Consumes("application/json")
    @Path("exitModule/{id}")
    @ApiOperation(
            value = "shutdown module with given ID"
    )
    @RequiresRoles(ADMIN)
    public Response exitModule(@PathParam("id") String id) {
        return service.exitModule(id);
    }

    @POST
    @Consumes("application/json")
    @Path("sendModuleCommand/{id}")
    @ApiOperation(
            value = "send command to module with given ID"
    )
    @RequiresRoles(ADMIN)
    public Response sendModuleCommand(@PathParam("id") String id, String commandString) {
        return service.sendModuleCommand(id, commandString);
    }
}
