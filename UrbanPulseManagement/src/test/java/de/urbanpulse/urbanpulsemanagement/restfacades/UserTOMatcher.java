package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class UserTOMatcher implements ArgumentMatcher<UserTO> {

    private final UserTO expectedUserTO;

    /**
     * <code>UserTO</code> argument that is equal to the given object. Equality is tested on <code>name</code>,
     * <code>password</code>, <code>permissions</code> and <code>roles</code> fields.
     * @param expectedUserTO the expected user
     * @return a new UserTomMatcher for expectedUserTO
     */
    static UserTO eq(UserTO expectedUserTO) {
        return argThat(new UserTOMatcher(expectedUserTO));
    }

    private UserTOMatcher(UserTO expectedUserTO) {
        this.expectedUserTO = expectedUserTO;
    }

    @Override
    public boolean matches(UserTO actualUserTO) {
        return Objects.equals(actualUserTO.getName(), expectedUserTO.getName()) &&
                Objects.equals(actualUserTO.getPassword(), expectedUserTO.getPassword()) &&
                Objects.equals(actualUserTO.getPermissions(), expectedUserTO.getPermissions()) &&
                Objects.equals(actualUserTO.getRoles(), expectedUserTO.getRoles());

    }
}
