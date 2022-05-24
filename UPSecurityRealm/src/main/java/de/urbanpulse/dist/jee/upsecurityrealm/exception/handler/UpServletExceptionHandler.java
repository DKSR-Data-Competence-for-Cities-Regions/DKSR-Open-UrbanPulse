package de.urbanpulse.dist.jee.upsecurityrealm.exception.handler;

import javax.servlet.ServletResponse;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface UpServletExceptionHandler<Ex extends Exception> {

    String CAUSE = "cause";

    boolean handle(ServletResponse response, Ex ex);

}
