package de.urbanpulse.dist.jee.upsecurityrealm;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UPCredentialsMatcherTest {

    UPCredentialsMatcher matcher;

    @Mock
    private AuthenticationInfo mockAuthenticationInfo;

    @Mock
    private AuthenticationToken mockAuthenticationToken;

    @Before
    public void initMatcher() {
        matcher = new UPCredentialsMatcher();
        // AuthenticationInfo shall return BCrypted Value of "myValidPassword":
        given(mockAuthenticationInfo.getCredentials()).willReturn("$2a$10$MX8tA2Dps/jn6o/3wWJ6Q.3b5wsavaijkyrPxb/EL02IjG8YOF7O2");
    }

    @Test
    public void test_doCredentialsMatch_matchesCorrectPassword() {
        String pwd = "myValidPassword";
        given(mockAuthenticationToken.getCredentials()).willReturn(pwd.toCharArray());

        assertTrue(matcher.doCredentialsMatch(mockAuthenticationToken, mockAuthenticationInfo));
    }

    @Test
    public void test_doCredentialsMatch_doesNotMatchIncorrectPassword() {
        String pwd = "notMyValidPassword";
        given(mockAuthenticationToken.getCredentials()).willReturn(pwd.toCharArray());

        assertFalse(matcher.doCredentialsMatch(mockAuthenticationToken, mockAuthenticationInfo));
    }

    @Test (expected = IllegalArgumentException.class)
    public void test_doCredentialsMatch_throwsExceptionForNonCharArray() {
        String pwd = "someString";
        given(mockAuthenticationToken.getCredentials()).willReturn(pwd);

        matcher.doCredentialsMatch(mockAuthenticationToken, mockAuthenticationInfo);
    }

}
