package de.urbanpulse.urbanpulsemanagement.restfacades;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsemanagement.restfacades.VersionRestFacade.VERSION_ROOT_PATH;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Api(tags = "version")
@Path(VERSION_ROOT_PATH)
public class VersionRestFacade {

    static final String VERSION_ROOT_PATH = "version";
    static final String VERSION_PROPERTY = "version";

    private final Logger LOG = LoggerFactory.getLogger(VersionRestFacade.class);

    @javax.ws.rs.core.Context
    private ServletContext context;

    private String version;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Retrieve version of UrbanPulseManagement",
            response = String.class
    )
    @RequiresRoles(ADMIN)
    public Response getVersionInfo() throws JsonProcessingException {

        if (version == null) {
          Properties prop = new Properties();
            try {
                prop.load(context.getResourceAsStream("/version.properties"));
                version = prop.getProperty(VERSION_PROPERTY);
                return Response.ok(version).build();
            } catch (IOException e) {
                LOG.error("Error during retrieving GAV coordinates!", e);
            }
        } else {
            return Response.ok(version).build();
        }

        return Response.status(500).entity("Unable to retrieve version info.").build();
    }

}
