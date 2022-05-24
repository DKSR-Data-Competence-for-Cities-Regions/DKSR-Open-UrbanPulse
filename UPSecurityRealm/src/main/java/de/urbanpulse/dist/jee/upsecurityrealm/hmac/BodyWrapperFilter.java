/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.web.servlet.OncePerRequestFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * This filter is used to apply the ServletRequestBodyFilter
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class BodyWrapperFilter extends OncePerRequestFilter {

        @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!(request instanceof ServletRequestBodyWrapper)) {
            HttpServletRequest httpRequest = WebUtils.toHttp(request);
            chain.doFilter(new ServletRequestBodyWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }




}
