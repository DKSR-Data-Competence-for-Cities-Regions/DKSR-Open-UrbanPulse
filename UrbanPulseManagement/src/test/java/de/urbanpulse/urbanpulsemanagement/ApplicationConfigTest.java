package de.urbanpulse.urbanpulsemanagement;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ApplicationConfigTest {

    @Test
    public void testSwaggerIsRegistered() {

        ApplicationConfig config = new ApplicationConfig();

        assertTrue(config.isRegistered(io.swagger.jaxrs.listing.ApiListingResource.class));
        assertTrue(config.isRegistered(io.swagger.jaxrs.listing.SwaggerSerializers.class));
    }
}
