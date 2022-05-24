package de.urbanpulse.urbanpulsemanagement.shiro;

import org.apache.shiro.web.servlet.ShiroFilter;

import javax.servlet.annotation.WebFilter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */

@WebFilter("/*")
public class ShiroFilterActivator extends ShiroFilter {

    private ShiroFilterActivator() {
    }
}
