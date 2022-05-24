package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ModuleUpdateManagerTest {

    @Mock
    private ResetModulesFacade resetModulesFacade;

    @InjectMocks
    private ModuleUpdateManager moduleUpdateManager;

    @Test
    public void test_handleNoHandlerModuleIDs_moduleNotFound_willContinueWithRemainingModules() {
        given(resetModulesFacade.resetModule(anyString())).willReturn(false);
        List<String> moduleIds = Arrays.asList("module1", "module2", "module3");
        boolean result = moduleUpdateManager.handleNoHandlerModuleIDs(moduleIds);
        assertFalse(result);
        verify(resetModulesFacade, times(3)).resetModule(anyString());
    }
}
