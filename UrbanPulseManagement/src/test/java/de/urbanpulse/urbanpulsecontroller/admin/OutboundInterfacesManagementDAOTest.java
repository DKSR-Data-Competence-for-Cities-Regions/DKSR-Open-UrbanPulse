package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class OutboundInterfacesManagementDAOTest {

    @Mock
    private UpdateListenerDAO mockListenerDao;

    @InjectMocks
    private OutboundInterfacesManagementDAO dao;

    @Mock
    private UpdateListenerTO mockListener;

    @Mock
    private List<UpdateListenerTO> mockListeners;

    @Mock
    private StatementEntity mockedStatementEntity;

    private static final String LISTENER_ID = "someId";
    private static final String STATEMENT_ID = "someOtherId";
    private static final String TARGET = "someTarget";
    private static final AuthJsonTO AUTH_JSON_TO = new AuthJsonTO();

    @Test
    public void createUpdateListener_returnsExpected() throws Exception {
        given(mockListenerDao.create(mockedStatementEntity, TARGET, AUTH_JSON_TO)).willReturn(mockListener);

        assertSame(mockListener, dao.createUpdateListener(mockedStatementEntity, TARGET, AUTH_JSON_TO));
    }

    @Test
    public void getUpdateListenerById_returnsExpected() throws Exception {
        given(mockListenerDao.getById(LISTENER_ID)).willReturn(mockListener);

        assertSame(mockListener, dao.getUpdateListenerById(LISTENER_ID));
    }

    @Test
    public void deleteUpdateListener_returnsExpected() throws Exception {
        given(mockListenerDao.deleteById(LISTENER_ID)).willReturn(LISTENER_ID);

        assertEquals(LISTENER_ID, dao.deleteUpdateListener(LISTENER_ID));
    }

    @Test
    public void getUpdateListenersForStatement_returnsExpected() throws Exception {
        given(mockListenerDao.getListenersOfStatement(STATEMENT_ID)).willReturn(mockListeners);
        assertSame(mockListeners, dao.getUpdateListenersOfStatement(STATEMENT_ID));
    }
}
