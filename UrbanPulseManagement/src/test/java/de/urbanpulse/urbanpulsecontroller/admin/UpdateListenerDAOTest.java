package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.UpdateListenerEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateListenerDAOTest {

    @Mock
    protected EntityManager entityManager;

    @InjectMocks
    private UpdateListenerDAO updateListenerDAO;

    @Mock
    UpdateListenerTO updateListenerTOMock;

    public UpdateListenerDAOTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreate_returnesExpected() throws Exception {
        StatementEntity statementEntity = new StatementEntity();
        statementEntity.setId("mystatement");
        statementEntity.setName("my statements name");
        statementEntity.setQuery("my query");
        statementEntity.setComment("This is just for testing");
        UpdateListenerTO returnedUpdateListenerTO = updateListenerDAO.create(statementEntity, "myTarget", null);

        verify(entityManager).persist(any(UpdateListenerEntity.class));
        verify(entityManager).flush();

        Assert.assertNotNull(returnedUpdateListenerTO);
        Assert.assertEquals("mystatement", returnedUpdateListenerTO.getStatementId());
        Assert.assertEquals("myTarget", returnedUpdateListenerTO.getTarget());
    }

    @Test
    public void testGetListenersOfStatement_forKnownStatement() throws Exception {
        String statementId = "123";

        StatementEntity statementEntity = new StatementEntity();
        statementEntity.setId(statementId);
        statementEntity.setName("my statements name");
        statementEntity.setQuery("my query");
        statementEntity.setComment("This is just for testing");

        List<UpdateListenerEntity> updateListeners = new ArrayList<>();
        UpdateListenerEntity listener1 = new UpdateListenerEntity();
        UpdateListenerEntity listener2 = new UpdateListenerEntity();
        updateListeners.add(listener1);
        updateListeners.add(listener2);
        statementEntity.setUpdateListeners(updateListeners);

        given(entityManager.find(any(), anyString())).willReturn(statementEntity);

        List<UpdateListenerTO> returnedListenerList = updateListenerDAO.getListenersOfStatement(statementId);

        verify(entityManager).find(StatementEntity.class, statementId);

        Assert.assertNotNull(returnedListenerList);
        Assert.assertEquals(2, returnedListenerList.size());
    }

    @Test
    public void testGetListenersOfStatement_forUnknownStatement() throws Exception {
        String statementId = "123";
        given(entityManager.find(any(), anyString())).willReturn(null);

        List<UpdateListenerTO> returnedListenerList = updateListenerDAO.getListenersOfStatement(statementId);

        verify(entityManager).find(StatementEntity.class, statementId);

        Assert.assertNotNull(returnedListenerList);
        Assert.assertEquals(0, returnedListenerList.size());
    }

}
