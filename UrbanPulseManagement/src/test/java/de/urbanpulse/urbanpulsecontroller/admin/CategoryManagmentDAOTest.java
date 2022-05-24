package de.urbanpulse.urbanpulsecontroller.admin;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.*;
import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.naming.OperationNotSupportedException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import static org.mockito.BDDMockito.given;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import static org.mockito.ArgumentMatchers.anyObject;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryManagmentDAOTest {

    private static final String CATEGORY_NAME = "myLittleMockCategory";
    private static final String CATEGORY_DESC = "this is myLittleMockCategory";

    private static final int PARENT_CATEGORY_ID = 42;
    private static final String PARENT_CATEGORY_ID_STRING = Integer.toString(PARENT_CATEGORY_ID);
    private final Class categoryEntityClass = CategoryEntity.class;

    @Mock
    protected EntityManager entityManager;

    @Mock
    CategoryTO parentTOMock;

    @Mock
    CategoryTO childTOMock;

    @Mock
    CategoryWithChildrenTO childWithCHildrenTOMock;

    @Mock
    CategoryEntity parentCategoryEntityMock;

    @Mock
    CategoryEntity childCategoryEntityMock;

    @Mock
    Query allRootCategoriesQueryMock;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    SensorEntity sensorEntity;

    @InjectMocks
    CategoryManagementDAO categoryManagmentDAO;

    public CategoryManagmentDAOTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Mock
    private SensorEntity mockSensor;

    @Test
    public void deleteById_removesFromSensorsThenDeletesAndFlushes_ifFound() throws Exception {
        List<SensorEntity> dummySensors = new LinkedList<>();
        dummySensors.add(mockSensor);

        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);
        given(parentCategoryEntityMock.getSensors()).willReturn(dummySensors);

        String idOfDeleted = categoryManagmentDAO.deleteById(PARENT_CATEGORY_ID_STRING);

        InOrder inOrder = inOrder(parentCategoryEntityMock, entityManager, parentCategoryEntityMock, mockSensor);
        inOrder.verify(mockSensor).removeCategory(parentCategoryEntityMock);
        inOrder.verify(entityManager).merge(mockSensor);
        inOrder.verify(entityManager).remove(parentCategoryEntityMock);
        inOrder.verify(entityManager).flush();
        assertEquals(PARENT_CATEGORY_ID_STRING, idOfDeleted);
    }

    @Test
    public void testGetChildCategories_returnExpected() {
        List<CategoryEntity> childCategories = new ArrayList<>();
        childCategories.add(childCategoryEntityMock);

        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);
        given(parentCategoryEntityMock.getChildCategories()).willReturn(childCategories);

        List<CategoryTO> resultList = categoryManagmentDAO.getChildCategories(parentTOMock);

        assertEquals(1, resultList.size());
        assertNotNull(resultList.get(0));
    }

    @Test
    public void testGetFullyResolvedChildCategories_returnExpected() {
        List<CategoryEntity> childCategories = new ArrayList<>();
        childCategories.add(childCategoryEntityMock);

        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);
        given(parentCategoryEntityMock.getChildCategories()).willReturn(childCategories);

        List<CategoryWithChildrenTO> resultList = categoryManagmentDAO.getFullyResolvedChildCategories(parentTOMock);

        Assert.assertTrue(resultList.size() == 1);
    }

    @Test
    public void testGetChildCategories_NoChildren_ShouldReturnEmptyList() {
        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);

        List<CategoryTO> resultList = categoryManagmentDAO.getChildCategories(parentTOMock);

        Assert.assertTrue(resultList.isEmpty());
    }

    @Test
    public void testGetFullyResolvedChildCategories_NoChildren_ShouldReturnEmptyList() {
        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);

        List<CategoryWithChildrenTO> resultList = categoryManagmentDAO.getFullyResolvedChildCategories(parentTOMock);

        Assert.assertTrue(resultList.isEmpty());
    }

    @Test
    public void testGetRootCategories_returnExpected() {
        List<CategoryEntity> rootCategories = new ArrayList<>();
        rootCategories.add(childCategoryEntityMock);

        given(entityManager.createNamedQuery(CategoryEntity.ALL_ROOT_CATEGORIES_QUERY)).willReturn(allRootCategoriesQueryMock);
        given(allRootCategoriesQueryMock.getResultList()).willReturn(rootCategories);

        List<CategoryTO> resultList = categoryManagmentDAO.getRootCategories();

        assertEquals(1, resultList.size());
        assertNotNull(resultList.get(0));
    }

    @Test
    public void testGetFullyResolvedRootCategories_returnExpected() {
        List<CategoryEntity> rootCategories = new ArrayList<>();
        rootCategories.add(childCategoryEntityMock);

        given(entityManager.createNamedQuery(CategoryEntity.ALL_ROOT_CATEGORIES_QUERY)).willReturn(allRootCategoriesQueryMock);
        given(allRootCategoriesQueryMock.getResultList()).willReturn(rootCategories);

        List<CategoryWithChildrenTO> resultList = categoryManagmentDAO.getFullyResolvedRootCategories();

        Assert.assertTrue(resultList.size() == 1);
    }

    @Test
    public void testResolveCategory_returnExpected() {
        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);

        CategoryWithChildrenTO result = categoryManagmentDAO.resolveCategory(parentTOMock);

        Assert.assertNotNull(result);
    }

    @Test
    public void testResolveCategory_returnsEmpty() {
        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(null);

        CategoryWithChildrenTO result = categoryManagmentDAO.resolveCategory(parentTOMock);

        Assert.assertNull(result);
    }

    @Test
    public void testGetParentCategory_HasParentCategory_ShouldReturnSomething() {
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(childCategoryEntityMock);
        given(childCategoryEntityMock.getParentCategory()).willReturn(new CategoryEntity());

        CategoryTO result = categoryManagmentDAO.getParentCategory(PARENT_CATEGORY_ID_STRING);

        Assert.assertNotNull(result);
    }

    @Test
    public void testGetParentCategory_NoParent_ShouldReturnNull() {
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(childCategoryEntityMock);
        given(childCategoryEntityMock.getParentCategory()).willReturn(null);

        CategoryTO result = categoryManagmentDAO.getParentCategory(PARENT_CATEGORY_ID_STRING);

        Assert.assertNull(result);
    }

    @Test
    public void testDeleteByIdWithDependencies_returnExpected() {
        List<CategoryEntity> childCategories = new ArrayList<>();
        childCategories.add(childCategoryEntityMock);

        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(childCategoryEntityMock);
        given(childCategoryEntityMock.getParentCategory()).willReturn(parentCategoryEntityMock);
        given(childCategoryEntityMock.getChildCategories()).willReturn(childCategories);

        String result = categoryManagmentDAO.deleteByIdWithDependencies(PARENT_CATEGORY_ID_STRING);

        verify(parentCategoryEntityMock).removeChild(childCategoryEntityMock);
        verify(childCategoryEntityMock).setParentCategory(null);
        verify(entityManager).persist(parentCategoryEntityMock);
        verify(entityManager).persist(childCategoryEntityMock);

        Assert.assertNotNull(result);
        Assert.assertEquals(PARENT_CATEGORY_ID_STRING, result);
    }

    @Test
    public void testDeleteByIdWithDependencies_returnsNull() {
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(null);

        String result = categoryManagmentDAO.deleteByIdWithDependencies(PARENT_CATEGORY_ID_STRING);

        Assert.assertNull(result);
    }

    @Test
    public void testCreateCategory_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException {
        List<String> childCategoryIds = new ArrayList<>();
        childCategoryIds.add(PARENT_CATEGORY_ID_STRING);

        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(childCategoryEntityMock);
        given(childCategoryEntityMock.getParentCategory()).willReturn(parentCategoryEntityMock);

        CategoryTO result = categoryManagmentDAO.createCategory(CATEGORY_NAME, CATEGORY_DESC, childCategoryIds);

        verify(parentCategoryEntityMock).removeChild(childCategoryEntityMock);
        verify(childCategoryEntityMock).setParentCategory((CategoryEntity) anyObject());
        verify(entityManager).persist(parentCategoryEntityMock);
        verify(entityManager).persist(childCategoryEntityMock);
        verify(entityManager).flush();

        Assert.assertNotNull(result);
    }

    @Test(expected = ReferencedEntityMissingException.class)
    public void testCreateCategory_throwsReferencedEntityMissingException() throws ReferencedEntityMissingException, CyclicDependencyException {
        List<String> childCategoryIds = new ArrayList<>();
        childCategoryIds.add(PARENT_CATEGORY_ID_STRING);

        CategoryTO result = categoryManagmentDAO.createCategory(CATEGORY_NAME, CATEGORY_DESC, childCategoryIds);

        Assert.assertNull(result);
    }

    @Test
    public void testUpdateCategory_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException, OperationNotSupportedException {
        List<String> childCategoryIds = new ArrayList<>();
        childCategoryIds.add(PARENT_CATEGORY_ID_STRING);

        given(parentTOMock.getId()).willReturn(PARENT_CATEGORY_ID_STRING);
        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(childCategoryEntityMock);
        given(childCategoryEntityMock.getParentCategory()).willReturn(parentCategoryEntityMock);

        categoryManagmentDAO.updateCategory(parentTOMock, CATEGORY_NAME, CATEGORY_DESC, childCategoryIds);

        verify(childCategoryEntityMock).setName(CATEGORY_NAME);
        verify(childCategoryEntityMock).setDescription(CATEGORY_DESC);

        verify(parentCategoryEntityMock).removeChild(childCategoryEntityMock);
        verify(childCategoryEntityMock).setParentCategory((CategoryEntity) anyObject());
        verify(entityManager).persist(parentCategoryEntityMock);
        verify(entityManager).flush();
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testUpdateCategory_throwsOperationNotSupportedException() throws ReferencedEntityMissingException, CyclicDependencyException, OperationNotSupportedException {
        List<String> childCategoryIds = new ArrayList<>();
        childCategoryIds.add(PARENT_CATEGORY_ID_STRING);

        categoryManagmentDAO.updateCategory(parentTOMock, CATEGORY_NAME, CATEGORY_DESC, childCategoryIds);
    }

    @Test
    public void testGetSensors_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException {
        List<CategoryEntity> children = new ArrayList<>();
        children.add(childCategoryEntityMock);

        List<SensorEntity> sensors = new ArrayList<>();
        sensors.add(sensorEntity);

        given(entityManager.find(categoryEntityClass, PARENT_CATEGORY_ID_STRING)).willReturn(parentCategoryEntityMock);
        given(parentCategoryEntityMock.getChildCategories()).willReturn(children);
        given(childCategoryEntityMock.getSensors()).willReturn(sensors);

        given(childCategoryEntityMock.getChildCategories())
                .willReturn(new ArrayList<>());

        Optional<List<SensorTO>> results = categoryManagmentDAO.getSensors("42");

        Assert.assertTrue(results.isPresent());
    }
}
