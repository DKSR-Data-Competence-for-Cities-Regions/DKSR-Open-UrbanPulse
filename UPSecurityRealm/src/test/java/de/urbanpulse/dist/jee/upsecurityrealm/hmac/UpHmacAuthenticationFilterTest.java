package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static de.urbanpulse.dist.jee.upsecurityrealm.hmac.UPHmacAuthenticationFilter.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UpHmacAuthenticationFilterTest {

    private static final String TEST_USER = "user";
    private static final String TEST_PASSWORD = "password";
    private static final String ENCODED_BASIC_CREDENTIALS = Base64.encodeToString((TEST_USER + ":" + TEST_PASSWORD).getBytes());
    private static final String ENCODED_INVALID_BASIC_CREDENTIALS = Base64.encodeToString((TEST_USER + ":").getBytes());
    private static final String HMAC_SIGNATURE = "h1CAQxEZawFcngV15hOKUDVnfMyLl52b1QLVaX6D8/I=";
    private static final String ACCESS_KEY = "YWRtaW4=";
    private static final String HMAC_CREDENTIALS = ACCESS_KEY + ":" + HMAC_SIGNATURE;

    @Mock
    private HttpServletRequest mockServletRequest;
    @Mock
    private HttpServletResponse mockServletResponse;

    private UPHmacAuthenticationFilter upHmacAuthenticationFilter;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void init() {
        reset(mockServletRequest, mockServletResponse);
        upHmacAuthenticationFilter = new UPHmacAuthenticationFilter();
    }

    @Test
    public void createTokenReturnsWithUsernamePasswordTokenForBasicAuthCredentials() throws Exception {
        when(mockServletRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BASIC_AUTH_SCHEMA + " " + ENCODED_BASIC_CREDENTIALS);

        UsernamePasswordToken token = (UsernamePasswordToken) upHmacAuthenticationFilter.createToken(mockServletRequest, mockServletResponse);

        assertThat(token.getUsername(), is(TEST_USER));
        assertThat(token.getPassword(), is(TEST_PASSWORD.toCharArray()));
    }

    @Test
    public void createTokenReturnsWithHmacTokenForHmacUserCredentials() throws Exception {
        when(mockServletRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(UPUSER_AUTH_SCHEMA + " " + HMAC_CREDENTIALS);

        HmacToken token = (HmacToken) upHmacAuthenticationFilter.createToken(mockServletRequest, mockServletResponse);

        assertThat(token.getSignature(), is(HMAC_SIGNATURE));
        assertThat(token.getAuthMode().name(), is(UPUSER_AUTH_SCHEMA));
        assertThat(token.getAccessKeyId(), is(ACCESS_KEY));
    }

    @Test
    public void createTokenWithNullForNullAuthorizationMode() throws Exception {
        assertNull(upHmacAuthenticationFilter.createToken(mockServletRequest, mockServletResponse));
    }

    @Test
    public void createTokenWithMissingUsernameOrPassword() throws Exception {
        when(mockServletRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BASIC_AUTH_SCHEMA + " " + ENCODED_INVALID_BASIC_CREDENTIALS);
        expectedEx.expect(IncorrectCredentialsException.class);
        expectedEx.expectMessage("User name and/or password is missing from Authorization header");
        HmacToken token = (HmacToken) upHmacAuthenticationFilter.createToken(mockServletRequest, mockServletResponse);
    }

    @Test
    public void onLoginFailureReturnsWithCauseResponseHeader() throws Exception {
        boolean result = upHmacAuthenticationFilter.onLoginFailure(null, new IncorrectCredentialsException("message"), mockServletRequest, mockServletResponse);

        assertFalse(result);
        verify(mockServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockServletResponse).addHeader("cause", "Incorrect credentials");
    }

}
