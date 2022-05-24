package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import static org.mockito.BDDMockito.*;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class EventTypeManagementDAOTest {

    @InjectMocks
    private EventTypeManagementDAO dao;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventTypeEntity mockEntity;

    @Mock
    private EntityManager mockEntityManager;

    private static final String ID = "12";
    private static final String NAME = "myLittleEventType";
    private static final String DESCRIPTION = "my little description";
    private static final String CONFIG = "{}";

    @Test
    public void test_getEventType_returnsExpected() throws Exception {
        EventTypeEntity testEventType = new EventTypeEntity();
        testEventType.setId(ID);

        given(mockEntityManager.find(EventTypeEntity.class, ID)).willReturn(testEventType);

        EventTypeTO entity = dao.getById(ID);

        assertEquals(testEventType.getId(), entity.getId());
    }

    @Test
    public void test_createEventType_createsAndPersistsEntityThenFlushes() throws Exception {
        EventTypeTO eventType = dao.createEventType(NAME, DESCRIPTION, CONFIG);

        InOrder inOrder = inOrder(mockEntityManager);
        inOrder.verify(mockEntityManager).persist(any(EventTypeEntity.class));
        inOrder.verify(mockEntityManager).flush();

        assertEquals(NAME, eventType.getName());
        assertEquals(DESCRIPTION, eventType.getDescription());
        assertEquals(CONFIG, eventType.getConfig());
    }

    @Test
    public void test_updateEventType_updatesAndPersistsEntityThenFlushesIfModified() throws Exception {
        EventTypeEntity testEventType = new EventTypeEntity();
        testEventType.setId(ID);

        given(mockEntityManager.find(EventTypeEntity.class, ID)).willReturn(testEventType);

        EventTypeTO updatedEventTypeTO = dao.updateEventType(ID, NAME, DESCRIPTION, CONFIG);

        assertEquals(ID, updatedEventTypeTO.getId());

        verify(mockEntityManager).persist(testEventType);
        verify(mockEntityManager).flush();
    }

    @Test
    public void test_updateEventType_returnsEventTypeButNeverPersistsOrFlushesIfNoChanges() throws Exception {
        EventTypeEntity testEventType = new EventTypeEntity();
        testEventType.setId(ID);

        given(mockEntityManager.find(EventTypeEntity.class, ID)).willReturn(testEventType);

        EventTypeTO updatedEventTypeTO = dao.updateEventType(ID, null, null, null);
        assertEquals(ID, updatedEventTypeTO.getId());

        verify(mockEntityManager, never()).persist(mockEntity);
        verify(mockEntityManager, never()).flush();
    }

    @Test
    public void test_deleteEventType_returnsNull_ifNotFound() throws Exception {
        given(mockEntityManager.find(EventTypeEntity.class, ID)).willReturn(null);

        assertNull(dao.deleteById(ID));
        verify(mockEntityManager, never()).remove(any());
    }

    @Test
    public void test_eventTypeExists_returnsFalse_ifNotExists() {
        TypedQuery query = mockNameQuery();

        given(query.getResultList()).willReturn(Collections.emptyList());
        boolean result = dao.eventTypeExists(NAME);
        assertFalse(result);
    }

    @Test
    public void test_eventTypeExists_returnsTrue_ifExists() {
        TypedQuery query = mockNameQuery();

        given(query.getResultList()).willReturn(Collections.singletonList(mockEntity));
        boolean result = dao.eventTypeExists(NAME);
        assertTrue(result);
    }

    private TypedQuery mockNameQuery() {
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        given(mockEntityManager.getCriteriaBuilder()).willReturn(criteriaBuilder);
        CriteriaQuery criteriaQuery = mock(CriteriaQuery.class);
        given(criteriaBuilder.createQuery()).willReturn(criteriaQuery);
        Root root = mock(Root.class);
        given(criteriaQuery.from(any(Class.class))).willReturn(root);
        Path path = mock(Path.class);
        given(root.get("name")).willReturn(path);
        Predicate predicate = mock(Predicate.class);
        given(criteriaBuilder.equal(eq(path), anyString())).willReturn(predicate);
        TypedQuery query = mock(TypedQuery.class);
        given(mockEntityManager.createQuery(criteriaQuery)).willReturn(query);
        return query;
    }
}
