package de.urbanpulse.urbanpulsemanagement.restfacades.swagger;

import de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles;
import io.swagger.jaxrs.ext.AbstractSwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.Operation;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import org.apache.shiro.authz.annotation.RequiresRoles;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UpSwaggerExtension extends AbstractSwaggerExtension {

    private static final Logger LOG = Logger.getLogger(UpSwaggerExtension.class.getName());
    private static final String BASIC_AUTH_MODE_STRING = "BASIC";
    private static final String HMAC_AUTH_MODE_STRING = "HMAC";

    /*
     * Please note that the timestamp header is NOT in date-time format for swagger because the timestamp format we
     * use is not conform with RFC3339. Instead the timestamp is interpreted as a string
     */
    private static final String TIMESTAMP_HEADER_VALUE = "The UrbanPulse-Timestamp should be defined in case of HMAC is used as the authorization mode. "
            + "It has to be provided in the following format: \\\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\\\" (e.g. \\\"2015-05-28T23:54:02.123+0000\\\"). "
            + "The time zone to use is UTC and the value must not differ more than 15 minutes from the current server time.";

    private static final String AUTHORIZATION_HEADER_TEMPLATE = "UrbanPulse authentication header can have multiple modes.";

    private static final String BASIC_AUTHORIZATION = " If Basic Auth is used, the value should be in the following format: Basic &lt;Base64-encoded username:password&gt;.";
    private static final String UP_AUTHORIZATION = " If HMAC is used with user authentication, the value should be in the following format:"
            + " UP base64(user name):hmac256(hash). " +
            "The hash is calculated over the timestamp + request body (for POST/PUT) or timestamp + request path (for GET/DELETE) " +
            "using the user's secret key.";
    private static final String CONNECTOR_AUTHORIZATION = " If HMAC is used with connector authentication, everything is the same as above; however, the connectors key and its ID is used";


    @Override
    public void decorateOperation(Operation operation, Method method, Iterator<SwaggerExtension> chain) {
        RequiresRoles rolesConfig = method.getAnnotation(RequiresRoles.class);
        List<String> roles = new ArrayList<>();
        if(rolesConfig != null) {
            roles = Arrays.asList(rolesConfig.value());
        }

        StringBuilder stringBuilder = new StringBuilder(AUTHORIZATION_HEADER_TEMPLATE);

        HeaderParameter timestampHeader = new HeaderParameter();
        timestampHeader.name("UrbanPulse-Timestamp").type("string").required(false).description(TIMESTAMP_HEADER_VALUE);
        operation.getParameters().add(timestampHeader);

        LOG.fine("Method name: " + method.getName());
        List<Parameter> parameterList = operation.getParameters();
        LOG.fine("Operation parameters:");
        for (Parameter parameter : parameterList) {
            LOG.fine("Name: " + parameter.getName() + "; in: " + parameter.getIn() + " ; req: " + parameter.getRequired());
        }

        LOG.fine("Adding auth mode: " + BASIC_AUTH_MODE_STRING);
        stringBuilder.append(BASIC_AUTHORIZATION);
        operation.addSecurity(BASIC_AUTH_MODE_STRING, Collections.emptyList());

        LOG.fine("Adding auth mode: " + HMAC_AUTH_MODE_STRING);
        stringBuilder.append(UP_AUTHORIZATION);
        operation.addSecurity(HMAC_AUTH_MODE_STRING, Collections.emptyList());

        /*if(roles.contains(UPDefaultRoles.CONNECTOR)) {
            LOG.fine("Adding documentation for: " + UPDefaultRoles.CONNECTOR);
            stringBuilder.append(CONNECTOR_AUTHORIZATION);
            operation.addSecurity(UPDefaultRoles.CONNECTOR, Collections.emptyList());
        }*/

        LOG.fine("Number of security elements for operation: " + operation.getSecurity().size());

        HeaderParameter authHeader = new HeaderParameter();
        authHeader.name("Authorization").type("string").required(false).description(stringBuilder.toString());
        operation.getParameters().add(authHeader);
    }
}

