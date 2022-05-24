package de.urbanpulse.urbanpulsemanagement.util;

import javax.ejb.ApplicationException;
import javax.ws.rs.WebApplicationException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@ApplicationException(rollback = true)
public class WrappedWebApplicationException extends RuntimeException {
    public WrappedWebApplicationException(WebApplicationException cause) {
        super(cause);
    }

    public WebApplicationException getWebApplicationException() {
        return (WebApplicationException) getCause();
    }
}
