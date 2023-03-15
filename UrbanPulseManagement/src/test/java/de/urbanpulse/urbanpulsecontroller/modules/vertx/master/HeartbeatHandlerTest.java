package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.UPModuleDAO;
import de.urbanpulse.urbanpulsecontroller.util.EmailSender;
import de.urbanpulse.util.status.UPModuleState;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class HeartbeatHandlerTest {

    @Mock
    private UPModuleDAO moduleDAO;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private HeartbeatHandler heartbeatHandler;

    @Before
    public void before() throws Exception {
    }

    @Test
    public void receivedHeartbeatForModule_sendMailForUnknownModule() {
        String id = "23423";
        heartbeatHandler.receivedHeartbeatForModule(id, UPModuleState.UNKNOWN);

        String subject = "Heartbeat";
        String body = id + ": heartbeat for unknown module";
        verify(emailSender).sendEmail(null, subject, body);
    }

    @Test
    public void receivedHeartbeatForModule_doNotSendMailForKnownModule() {
        String id = "23423";
        given(moduleDAO.queryById(id)).willReturn(new UPModuleEntity());

        heartbeatHandler.receivedHeartbeatForModule(id, UPModuleState.UNKNOWN);

        verify(emailSender, Mockito.times(0)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void sendMailAfterModuleIsLiveAgain() {
        String id = "23423";
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(false);
        module.setLastHeartbeat(new Date(0));
        System.out.println(module.getLastHeartbeat().toGMTString());
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);
        given(moduleDAO.queryById(id)).willReturn(module);
        given(emailSender.sendEmail(any(), anyString(), anyString())).willReturn(true);

        heartbeatHandler.checkModules();
        assertTrue(module.isMailSent());
        heartbeatHandler.receivedHeartbeatForModule(id, UPModuleState.UNKNOWN);
        assertFalse(module.isMailSent());

        verify(emailSender, Mockito.times(2)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void checkModules_doNotSendMailForValidHeartbeat() {
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(false);
        module.setLastHeartbeat(new Date());
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);

        heartbeatHandler.checkModules();
        assertFalse(module.isMailSent());
        verify(emailSender, Mockito.times(0)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void checkModules_sendMailForNoHeartbeat() {
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(false);
        module.setLastHeartbeat(null);
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);
        given(emailSender.sendEmail(any(), anyString(), anyString())).willReturn(true);

        heartbeatHandler.checkModules();
        assertTrue(module.isMailSent());
        verify(emailSender, Mockito.times(1)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void checkModules_tryToResendMail() {
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(false);
        module.setLastHeartbeat(new Date(0));
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);
        given(emailSender.sendEmail(any(), anyString(), anyString())).willReturn(false);

        heartbeatHandler.checkModules();
        assertFalse(module.isMailSent());

        given(emailSender.sendEmail(any(), anyString(), anyString())).willReturn(true);
        heartbeatHandler.checkModules();
        assertTrue(module.isMailSent());

        verify(emailSender, Mockito.times(2)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void checkModules_sendMailForOldHeartbeat() {
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(false);
        module.setLastHeartbeat(new Date(0));
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);
        given(emailSender.sendEmail(any(), anyString(), anyString())).willReturn(true);

        heartbeatHandler.checkModules();
        assertTrue(module.isMailSent());
        verify(emailSender, Mockito.times(1)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    public void checkModules_DoNotSendMailForAlreadySentMail() {
        List<UPModuleEntity> list = new ArrayList<>();
        UPModuleEntity module = new UPModuleEntity();
        module.setMailSent(true);
        module.setLastHeartbeat(null);
        list.add(module);
        given(moduleDAO.queryAll()).willReturn(list);

        heartbeatHandler.checkModules();
        assertTrue(module.isMailSent());
        verify(emailSender, Mockito.times(0)).sendEmail(any(), anyString(), anyString());
    }
}
