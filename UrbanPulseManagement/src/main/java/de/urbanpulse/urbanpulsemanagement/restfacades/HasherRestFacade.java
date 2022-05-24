package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.dist.jee.upsecurityrealm.hmac.Hasher;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.HasherInputTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.HasherOutputTO;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import city.ui.shared.commons.time.UPDateTimeFormat;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.authz.annotation.RequiresRoles;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path("hasher")
@Api
public class HasherRestFacade extends AbstractRestFacade {

    @POST
    @RequiresRoles(ADMIN)
    @ApiOperation(
            value = "Get the Authorization and UrbanPulse-Timestamp header for further custom use.",
            response = HasherOutputTO.class
    )
    public Response hash(@HeaderParam("Authorization") @ApiParam(hidden = true) String authHeader, HasherInputTO payload) {
        String urbanPulseTimestamp = OffsetDateTime.now(ZoneOffset.UTC).format(UPDateTimeFormat.getFormatterWithZoneZ());
        String toHash;

        if (HttpMethod.POST.equalsIgnoreCase(payload.getMethod()) || HttpMethod.PUT.equalsIgnoreCase(payload.getMethod())) {
            if (payload.getBody() == null) {
                return Response.status(Status.BAD_REQUEST).entity("body must be given for POST/PUT request").build();
            }

            toHash = urbanPulseTimestamp + payload.getBody();
        } else if (HttpMethod.GET.equalsIgnoreCase(payload.getMethod()) || HttpMethod.DELETE.equalsIgnoreCase(payload.getMethod())) {
            if (payload.getPath() == null) {
                return Response.status(Status.BAD_REQUEST).entity("path must be given for POST/PUT request").build();
            }

            toHash = urbanPulseTimestamp + payload.getPath();
        } else {
            return Response.status(Status.BAD_REQUEST).entity("unsupported request method").build();
        }

        if (payload.getSecretKey() == null) {
            return Response.status(Status.BAD_REQUEST).entity("secret key must be provided").build();
        }

        String hash;
        try {
            hash = Hasher.createHmac256(payload.getSecretKey(), toHash);
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }

        String user = getUserFromAuthorizationHeaderString(authHeader.replaceFirst("Basic", ""));
        String userBase64 = encodeBase64(user);

        HasherOutputTO hasherOutput = new HasherOutputTO();
        hasherOutput.setTimestampHeader("UrbanPulse-Timestamp: " + urbanPulseTimestamp);
        hasherOutput.setAuthorizationHeader(String.format("%s: UP %s:%s", HttpHeaders.AUTHORIZATION, userBase64, hash));

        return Response.ok(hasherOutput).build();
    }

    protected String encodeBase64(String str) {
        byte[] b64ByteArray = Base64.encodeBase64(str.getBytes(StandardCharsets.UTF_8));
        return new String(b64ByteArray, StandardCharsets.US_ASCII);
    }

    protected String getUserFromAuthorizationHeaderString(String headerStr) {
        String usernameColonPassword = new String(Base64.decodeBase64(headerStr.trim()));

        String[] split = usernameColonPassword.split(":", 2);
        return split[0];
    }
}
