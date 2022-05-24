package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.OperationNotSupportedException;
import javax.persistence.EntityManager;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectorManagementDAOTest {

    @InjectMocks
    private ConnectorManagementDAO dao;

    @Mock
    private EntityManager mockEntityManager;

    private static final String ID = "00000000-0000-0000-0000-000000000013";
    private static final String DESCRIPTION = "my little description";

    @Test
    public void getById_returnsExpected() throws Exception {
        ConnectorEntity testConnector = new ConnectorEntity();
        testConnector.setId(ID);
        testConnector.setDescription(DESCRIPTION);

        given(mockEntityManager.find(ConnectorEntity.class, ID)).willReturn(testConnector);

        ConnectorTO connector = dao.getById(ID);

        assertThat(connector.getId(), is(ID));
        assertThat(connector.getDescription(), is(DESCRIPTION));
    }

    @Test
    public void createConnector_createsKeyThenPersistsEntityThenFlushes() throws Exception {
        dao.createConnector(DESCRIPTION);

        InOrder inOrder = inOrder(mockEntityManager);
        inOrder.verify(mockEntityManager).persist(any(ConnectorEntity.class));
        inOrder.verify(mockEntityManager).flush();
    }

    @Test
    public void updateConnector_setsDescriptionThenMergesThenFlushesAndReturnsTO() throws OperationNotSupportedException {
        ConnectorEntity testConnector = new ConnectorEntity();
        ConnectorEntity testConnectorSpy = spy(testConnector);

        ConnectorEntity testMergedConnector = new ConnectorEntity();
        testMergedConnector.setDescription(DESCRIPTION);

        given(mockEntityManager.find(ConnectorEntity.class, ID)).willReturn(testConnectorSpy);
        given(mockEntityManager.merge(testConnectorSpy)).willReturn(testMergedConnector);

        ConnectorTO connector = dao.updateConnector(ID, DESCRIPTION);
        assertThat(connector.getId(), is(testMergedConnector.getId()));

        InOrder inOrder = inOrder(mockEntityManager, testConnectorSpy);
        inOrder.verify(testConnectorSpy).setDescription(DESCRIPTION);
        inOrder.verify(mockEntityManager).merge(testConnectorSpy);
        inOrder.verify(mockEntityManager).flush();
    }

    @Test(expected = OperationNotSupportedException.class)
    public void updateConnector_throwsOperationNotSupportedException() throws OperationNotSupportedException {
        given(mockEntityManager.find(ConnectorEntity.class, ID)).willReturn(null);

        dao.updateConnector(ID, DESCRIPTION);
    }
}
