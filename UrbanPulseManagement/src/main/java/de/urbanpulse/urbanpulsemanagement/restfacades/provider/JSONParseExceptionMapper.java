package de.urbanpulse.urbanpulsemanagement.restfacades.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Provider
class JSONParseExceptionMapper implements ExceptionMapper<javax.json.stream.JsonParsingException> {
    @Override
    public Response toResponse(final javax.json.stream.JsonParsingException jpe) {
        return Response
                .status(Status.BAD_REQUEST)
                .entity("Invalid format")
                .build();
    }
}
