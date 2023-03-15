package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.VirtualSensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class VirtualSensorManagementDAOTest {

    private static final String CATEGORY_ID = "13";
    private static final String RESULT_STATEMENT_ID = "4711";
    private static final String RESULT_EVENTTYPE_ID = "4712";
    private static final String STATEMENT_IDS = "[]";
    private static final String EVENT_TYPE_ID = "23";
    private static final String DESCRIPTION = "{}";

    @InjectMocks
    public VirtualSensorManagementDAO dao;

    @Mock
    private EntityManager mockEntityManager;

    @Mock
    private CategoryManagementDAO mockCategoryManagmentDAO;

    @Mock
    private StatementManagementDAO mockStatementManagementDAO;

    @Mock
    private CategoryEntity mockCategory;

    @Mock
    private StatementEntity mockResultStatement;

    @Mock
    private VirtualSensorTO mockTO;

    @Mock
    private Query mockQuery;

    @Mock
    private TypedQuery<VirtualSensorEntity> mockTypedQuery;

    private static final String RESULT_STATEMENT_NAME = "my little resultStatement";

    private static final String QUERY_TEMPLATE = "select whatever from <SID_PLACEHOLDER>";

    // "+ null" because our mockEntityManager cannot set the ID of the entity in persist(), so it remains null
    private static final String EXPECTED_QUERY = "select whatever from " + null;

    @Mock
    private List mockResultList;

    @Mock
    private List<VirtualSensorTO> mockVirtualSensors;

    @Before
    public void setUp() {
        given(mockCategoryManagmentDAO.queryById(CATEGORY_ID)).willReturn(mockCategory);
        given(mockStatementManagementDAO.queryById(RESULT_STATEMENT_ID)).willReturn(mockResultStatement);
    }

    @Test
    public void getFilteredBySchema_test() {
        List<VirtualSensorEntity> vsList = new ArrayList();
        VirtualSensorEntity vse = new VirtualSensorEntity();
        vse.setId("ABC");
        vse.setDescription("{}");
        EventTypeEntity et = new EventTypeEntity();
        et.setId("DEF");
        StatementEntity se = new StatementEntity();
        se.setId("GHI");
        CategoryEntity ce = new CategoryEntity();
        ce.setId("JKL");
        vse.setResultEventType(et);
        vse.setResultStatement(se);
        vse.setCategory(ce);
        vsList.add(vse);
        given(mockTypedQuery.setParameter(anyString(), anyString())).willReturn(mockTypedQuery);
        given(mockEntityManager.createQuery(anyString(), eq(VirtualSensorEntity.class))).willReturn(mockTypedQuery);
        given(mockTypedQuery.getResultList()).willReturn(vsList);
        List<VirtualSensorTO> result = dao.getFilteredBySchema("THE_SCHEMA");
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void getFilteredByResultStatementName_returnsResultAsList() throws Exception {
        List<VirtualSensorEntity> testSensorList = new ArrayList<>();
        testSensorList.add(new VirtualSensorEntity());
        testSensorList.add(new VirtualSensorEntity());

        given(mockEntityManager.createNamedQuery("getByResultStatementName")).willReturn(mockQuery);

        given(mockQuery.getResultList()).willReturn(testSensorList);

        List<VirtualSensorTO> virtualSensors = dao.getFilteredByResultStatementName(RESULT_STATEMENT_NAME);

        verify(mockQuery).setParameter("resultStatementName", RESULT_STATEMENT_NAME);

        assertEquals(testSensorList.size(), virtualSensors.size());
    }

    @Test(expected = ReferencedEntityMissingException.class)
    public void createVirtualSensor_throwsIfResultStatementNotFound() throws ReferencedEntityMissingException {
        given(mockStatementManagementDAO.queryById(RESULT_STATEMENT_ID)).willReturn(null);

        dao.createVirtualSensorAndReplaceSidPlaceholder(CATEGORY_ID, RESULT_STATEMENT_ID, STATEMENT_IDS, DESCRIPTION, EVENT_TYPE_ID, RESULT_EVENTTYPE_ID, new ArrayList<>());

        verifyNoInteractions(mockEntityManager);
    }

    @Test(expected = ReferencedEntityMissingException.class)
    public void createVirtualSensor_throwsIfCategoryNotFound() throws ReferencedEntityMissingException {
        given(mockCategoryManagmentDAO.queryById(CATEGORY_ID)).willReturn(null);

        dao.createVirtualSensorAndReplaceSidPlaceholder(CATEGORY_ID, RESULT_STATEMENT_ID, STATEMENT_IDS, DESCRIPTION, EVENT_TYPE_ID, RESULT_EVENTTYPE_ID, new ArrayList<>());

        verifyNoInteractions(mockEntityManager);
    }

}
