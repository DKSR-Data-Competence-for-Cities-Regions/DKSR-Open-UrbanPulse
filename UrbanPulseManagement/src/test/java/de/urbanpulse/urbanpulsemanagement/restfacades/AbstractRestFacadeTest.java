package de.urbanpulse.urbanpulsemanagement.restfacades;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.BeforeClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import org.apache.shiro.mgt.SecurityManager;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractRestFacadeTest {

    // Static mocks - will be mocked in setUpClass
    protected static org.apache.shiro.mgt.SecurityManager securityManager;
    protected static Subject subject;

    @BeforeClass
    public static void setUpClass() {
        // These things must only be mocked once (!!!), as they are used in a static context
        if (subject == null) {
            subject = mock(Subject.class);
        }

        if (securityManager == null) {
            securityManager = mock(SecurityManager.class);
            SecurityUtils.setSecurityManager(securityManager);
            given(securityManager.createSubject(any())).willReturn(subject);
        }
    }

}
