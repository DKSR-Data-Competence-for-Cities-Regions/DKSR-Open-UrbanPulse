package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsemanagement.restfacades.swagger.ConnectorAuthDefinition;
import de.urbanpulse.urbanpulsemanagement.restfacades.swagger.HmacAuthDefinition;
import de.urbanpulse.urbanpulsemanagement.restfacades.swagger.UpSwaggerExtension;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.Swagger;
import io.swagger.models.auth.BasicAuthDefinition;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@SwaggerDefinition
public class ApiListener implements ReaderListener {

    public ApiListener() {
        SwaggerExtensions.getExtensions().add(new UpSwaggerExtension());
    }

    @Override
    public void beforeScan(Reader reader, Swagger swagger) {
        // Do nothing
    }

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        try {
            setSecurityDefinitions(swagger);
        } catch (Exception ex) {
            Logger.getLogger(ApiListener.class.getName()).log(Level.SEVERE, "Error generating Swagger definitions!", ex);
        }
    }

    private void setSecurityDefinitions(Swagger swagger) {
        BasicAuthDefinition basicAuthDefinition = new BasicAuthDefinition();
        basicAuthDefinition.setDescription("Authenticate using basic authorization");
        swagger.addSecurityDefinition("BASIC", basicAuthDefinition);

        HmacAuthDefinition hmacAuthDefinition = new HmacAuthDefinition();
        hmacAuthDefinition.setDescription("Hash-based message authentication");
        swagger.addSecurityDefinition("HMAC", hmacAuthDefinition);

        ConnectorAuthDefinition connectorAuthDefinition = new ConnectorAuthDefinition();
        connectorAuthDefinition.setDescription("UrbanPulse Connector authentication");
        swagger.addSecurityDefinition("connector", connectorAuthDefinition);
    }

}
