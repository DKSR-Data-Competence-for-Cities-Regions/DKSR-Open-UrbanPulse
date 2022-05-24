package de.urbanpulse.urbanpulsemanagement;

import io.swagger.jaxrs.config.BeanConfig;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;


/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {

    private static final String API_VERSION = "1.0";

    public ApplicationConfig() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("[ui!] UrbanPulse");
        beanConfig.setVersion(API_VERSION);
        beanConfig.setDescription("API for the management of the configuration of [ui!] UrbanPulse modules");
        beanConfig.setSchemes(new String[]{"https"});
        beanConfig.setBasePath("/UrbanPulseManagement/api");
        beanConfig.setResourcePackage("de.urbanpulse.urbanpulsemanagement.restfacades");
        beanConfig.setScan(true);

        register(JacksonFeature.class).packages("de.urbanpulse.urbanpulsemanagement.restfacades");

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(org.apache.shiro.web.jaxrs.ShiroFeature.class);
    }

}
