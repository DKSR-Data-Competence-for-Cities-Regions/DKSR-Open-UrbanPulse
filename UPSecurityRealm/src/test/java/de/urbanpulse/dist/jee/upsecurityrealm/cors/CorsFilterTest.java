package de.urbanpulse.dist.jee.upsecurityrealm.cors;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CorsFilterTest {

    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private FilterChain filterChain;

    private CorsFilter corsFilter;

    @Before
    public void setUp() throws Exception {
        corsFilter = new CorsFilter();
        reset(servletRequest, servletResponse, filterChain);
    }

    @Test
    public void filterShouldAddCorsResponseHeadersForPreflightRequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("OPTIONS");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verifyZeroInteractions(filterChain);
    }

    @Test
    public void filterShouldAddCorsResponseHeadersForGETRequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verify(filterChain,atLeastOnce()).doFilter(servletRequest, servletResponse);

    }

    @Test
    public void filterShouldAddCorsResponseHeadersForPOSTRequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verify(filterChain,atLeastOnce()).doFilter(servletRequest, servletResponse);

    }

    @Test
    public void filterShouldAddCorsResponseHeadersForPUTRequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("PUT");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verify(filterChain,atLeastOnce()).doFilter(servletRequest, servletResponse);

    }

    @Test
    public void filterShouldAddCorsResponseHeadersForDELETERequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("DELETE");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verify(filterChain,atLeastOnce()).doFilter(servletRequest, servletResponse);

    }

    @Test
    public void filterShouldAddCorsResponseHeadersForHEADRequest() throws Exception {
        when(servletRequest.getMethod()).thenReturn("HEAD");
        when(servletRequest.getHeader("Origin")).thenReturn("sampleOrigin");

        corsFilter.doFilterInternal(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,UrbanPulse-Timestamp");
        verify(servletResponse).setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,HEAD,POST,PUT,DELETE");
        verify(servletResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(servletResponse).setHeader("Access-Control-Allow-Origin", "https://*.urbanpulse.de");
        verify(filterChain,atLeastOnce()).doFilter(servletRequest, servletResponse);

    }

}
