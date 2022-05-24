package de.urbanpulse.urbanpulsemanagement.util;

import java.util.regex.Pattern;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * similar to http://www.mkyong.com/regular-expressions/how-to-validate-password-with-regular-expression/, but unlike that example
 * we do not enforce a max length on passwords
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
public class PasswordPolicy {

    public static final String PASSWORD_POLICY_HUMAN_READABLE = "0-9, a-z, A-Z, !?\\§|/°+~=:;(){}[]^<>@#.,-_$%";

    private static final String PASSWORD_REGEX
            = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[\\!\\?\\\\§\\|/°\\+\\~\\=\\:;\\(\\){}\\[\\]\\:\\^<>@#\\.,\\-_$%]).{8,})";

    private final Pattern pattern = Pattern.compile(PASSWORD_REGEX);

    /**
     * @param password the password string
     * @return true if the password has 8+ chars, containing at least of of each: a-z, A-Z, 0-9, %$§°^.:;-=_#~+@\/|&lt;&gt;[]{}()
     */
    public boolean isAcceptable(String password) {
        if (password == null) {
            return false;
        }

        return pattern.matcher(password).matches();
    }
}
