package de.urbanpulse.urbanpulsemanagement.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class PasswordPolicyTest {

    PasswordPolicy policy;

    @Before
    public void setUp() {
        policy = new PasswordPolicy();
    }

    @Test
    public void forbidNull() throws Exception {
        assertFalse(policy.isAcceptable(null));
    }

    @Test
    public void forbidEmpty() throws Exception {
        assertFalse(policy.isAcceptable(""));
    }

    @Test
    public void forbid7Char() throws Exception {
        assertFalse(policy.isAcceptable("a.8Zugw"));
    }

    @Test
    public void allowStrongIsh8Char() throws Exception {
        assertTrue(policy.isAcceptable("a.8Zugwu"));
    }

    @Test
    public void allowCommonSpecialCharsIfRestIsOk() throws Exception {
        assertTrue(policy.isAcceptable("a%8Zugwu"));
        assertTrue(policy.isAcceptable("a$8Zugwu"));
        assertTrue(policy.isAcceptable("a§8Zugwu"));
        assertTrue(policy.isAcceptable("a°8Zugwu"));
        assertTrue(policy.isAcceptable("a^8Zugwu"));
        assertTrue(policy.isAcceptable("a.8Zugwu"));
        assertTrue(policy.isAcceptable("a:8Zugwu"));
        assertTrue(policy.isAcceptable("a;8Zugwu"));
        assertTrue(policy.isAcceptable("a-8Zugwu"));
        assertTrue(policy.isAcceptable("a=8Zugwu"));
        assertTrue(policy.isAcceptable("a_8Zugwu"));
        assertTrue(policy.isAcceptable("a#8Zugwu"));
        assertTrue(policy.isAcceptable("a~8Zugwu"));
        assertTrue(policy.isAcceptable("a+8Zugwu"));
        assertTrue(policy.isAcceptable("a@8Zugwu"));
        assertTrue(policy.isAcceptable("a\\8Zugwu"));
        assertTrue(policy.isAcceptable("a/8Zugwu"));
        assertTrue(policy.isAcceptable("a|8Zugwu"));
        assertTrue(policy.isAcceptable("a<8Zugwu"));
        assertTrue(policy.isAcceptable("a>8Zugwu"));
        assertTrue(policy.isAcceptable("a[8Zugwu"));
        assertTrue(policy.isAcceptable("a]8Zugwu"));
        assertTrue(policy.isAcceptable("a{8Zugwu"));
        assertTrue(policy.isAcceptable("a}8Zugwu"));
        assertTrue(policy.isAcceptable("a(8Zugwu"));
        assertTrue(policy.isAcceptable("a)8Zugwu"));
    }

    @Test
    public void allowReallyLong() throws Exception {
        assertTrue(policy.isAcceptable("a.8Zugwuhgklfegvlwkrvgewlirteuvgdcelgklejbvuewgrbilvgbqweilrvgegilrvceuglckwgil5ruevcgeq"));
    }
}
