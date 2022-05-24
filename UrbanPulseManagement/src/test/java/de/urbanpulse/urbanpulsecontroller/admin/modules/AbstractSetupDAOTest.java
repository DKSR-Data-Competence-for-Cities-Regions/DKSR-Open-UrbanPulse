package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.AbstractModuleSetupEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractSetupDAOTest {

    private static final String MODULE_ID = "aModuleId";

    @InjectMocks
    private TestSetupDAO setupDAO;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery criteriaQuery;

    @Mock
    private Root<AbstractModuleSetupEntity> root;

    @Mock
    private TypedQuery<AbstractModuleSetupEntity> typedQuery;

    @Mock
    private Path moduleIdPath;

    @Mock
    private AbstractModuleSetupEntity resultEntity;

    @Before
    public void preconditions() {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilder);
        given(criteriaBuilder.createQuery()).willReturn(criteriaQuery);

        given(criteriaQuery.from(any(Class.class))).willReturn(root);
        given(entityManager.createQuery(criteriaQuery)).willReturn(typedQuery);
        given(typedQuery.setMaxResults(anyInt())).willReturn(typedQuery);
        given(typedQuery.setLockMode(any())).willReturn(typedQuery);
        given(typedQuery.setHint(anyString(), any())).willReturn(typedQuery);
        given(root.get("moduleId")).willReturn(moduleIdPath);

        given(entityManager.merge(resultEntity)).willReturn(resultEntity);
    }

    @Test
    public void test_getAndAssignConfig_willReturnNull_ifNoConfigAvailableAndModuleNotYetAssigned() {
        given(typedQuery.getResultList()).willReturn(Collections.emptyList());

        AbstractModuleSetupEntity result = setupDAO.getAndAssignConfig(MODULE_ID);
        assertNull(result);

        //Make sure it was called once with an equals MODULE_ID and then with isNull
        verify(criteriaBuilder, times(2)).createQuery();
        verify(criteriaBuilder, times(1)).equal(moduleIdPath, MODULE_ID);
        verify(criteriaBuilder, times(1)).isNull(moduleIdPath);
        verify(typedQuery, times(2)).getResultList();
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @Test
    public void test_getAndAssignConfig_willReturnAssignedConfig_ifModuleAssigned() {
        given(typedQuery.getResultList()).willReturn(Collections.singletonList(resultEntity));

        AbstractModuleSetupEntity result = setupDAO.getAndAssignConfig(MODULE_ID);
        assertEquals(resultEntity, result);

        //Make sure it was called once with an equals MODULE_ID and then never again
        verify(criteriaBuilder, times(1)).createQuery();
        verify(criteriaBuilder, times(1)).equal(moduleIdPath, MODULE_ID);
        verify(criteriaBuilder, never()).isNull(moduleIdPath);
        verify(typedQuery, times(1)).getResultList();
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @Test
    public void test_getAndAssignConfig_willAssignConfig_ifConfigAvailableAndModuleNotYetAssigned() {
        doAnswer(new Answer<List<AbstractModuleSetupEntity>>() {
            private int callNo = 0;

            @Override
            public List<AbstractModuleSetupEntity> answer(InvocationOnMock invocation) throws Throwable {
                if (callNo++ == 0) {
                    return Collections.emptyList();
                } else {
                    return Collections.singletonList(resultEntity);
                }
            }
        }).when(typedQuery).getResultList();

        AbstractModuleSetupEntity result = setupDAO.getAndAssignConfig(MODULE_ID);
        assertEquals(resultEntity, result);

        //Make sure it was called once with an equals MODULE_ID and then with isNull
        verify(criteriaBuilder, times(2)).createQuery();
        verify(criteriaBuilder, times(1)).equal(moduleIdPath, MODULE_ID);
        verify(criteriaBuilder, times(1)).isNull(moduleIdPath);
        verify(typedQuery, times(2)).getResultList();
        verifyNoMoreInteractions(criteriaBuilder);

        verify(resultEntity).setModuleId(MODULE_ID);
    }

    static class TestSetupDAO extends AbstractSetupDAO<AbstractModuleSetupEntity> {

        @Override
        protected Class getClazz() {
            return AbstractModuleSetupEntity.class;
        }

        @Override
        public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UPModuleType getModuleType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
