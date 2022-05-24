package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.BDDMockito.given;

import org.mockito.InjectMocks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissionsManagementDAOTest {

    private static final long PERMISSION_ID = 4711;
    private static final String PERMISSION_ID_STRING = Long.toString(PERMISSION_ID);

    @Mock
    protected EntityManager entityManager;
    @Mock
    PermissionEntity permissionMock;
    @Mock
    PermissionTO permissionTOMock;
    @Mock
    CriteriaQuery criteriaQueryMock;
    @Mock
    TypedQuery queryMock;
    @Mock
    Root rootMock;
    @Mock
    Path columnPathMock;
    @Mock
    Predicate predicteMock;
    @InjectMocks
    private PermissionManagementDAO permissionDao;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreatePermission_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(PermissionEntity.class)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);
        given(queryMock.getResultList()).willReturn(new ArrayList<>());

        PermissionTO permissionTO = new PermissionTO("gee", "Permission to ask permission");
        PermissionTO returnedPermissionTO = permissionDao.createPermission(permissionTO);

        verify(entityManager).persist(any(PermissionEntity.class));

        assertNotNull(returnedPermissionTO);
    }

    @Test
    public void testGetAll_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(PermissionEntity.class)).willReturn(rootMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);

        List<PermissionEntity> existingPermissions = new ArrayList<>();
        existingPermissions.add(permissionMock);
        given(queryMock.getResultList()).willReturn(existingPermissions);

        List<PermissionTO> returnedPermissions = permissionDao.getAll();

        assertNotNull(returnedPermissions);
        assertEquals(1, returnedPermissions.size());
    }

    @Test
    public void testDeleteById_returnsExpected() throws Exception {
        given(entityManager.find(PermissionEntity.class, PERMISSION_ID_STRING)).willReturn(permissionMock);

        String deletedPermissionId = permissionDao.deleteById(PERMISSION_ID_STRING);

        verify(entityManager, times(1)).remove(permissionMock);

        assertEquals(PERMISSION_ID_STRING, deletedPermissionId);
    }

    @Test
    public void testDeleteById_noPermissionFound() throws Exception {

        String deletedPermissionId = permissionDao.deleteById(PERMISSION_ID_STRING);

        verify(entityManager, never()).remove(permissionMock);

        assertNull(deletedPermissionId);
    }

    @Test
    public void test_getUserPermissionsBySensorId() {
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        PermissionEntity entity = new PermissionEntity("123", "sensor:SID1:livedata:read");
        permissionEntities.add(entity);
        TypedQuery<PermissionEntity> mockedTypedQuery = Mockito.mock(TypedQuery.class);
        given(entityManager.createNamedQuery("getUserPermissionsBySensorId", PermissionEntity.class)).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.setParameter("userId", "1")).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.setParameter("sensorId", "SID1")).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.getResultList()).willReturn(permissionEntities);

        List<PermissionTO> permissionTOList = permissionDao.getUserPermissionsBySensorId("1", "SID1");

        Assert.assertEquals(1, permissionTOList.size());
        Assert.assertEquals("sensor:SID1:livedata:read", permissionTOList.get(0).getName());
    }

    @Test
    public void test_getRolePermissionsBySensorId() {
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        PermissionEntity entity = new PermissionEntity("123", "sensor:SID1:livedata:read");
        permissionEntities.add(entity);
        TypedQuery<PermissionEntity> mockedTypedQuery = Mockito.mock(TypedQuery.class);
        given(entityManager.createNamedQuery("getRolePermissionsBySensorId", PermissionEntity.class)).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.setParameter("roleId", "1")).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.setParameter("sensorId", "SID1")).willReturn(mockedTypedQuery);
        given(mockedTypedQuery.getResultList()).willReturn(permissionEntities);

        List<PermissionTO> permissionTOList = permissionDao.getRolePermissionsBySensorId("1", "SID1");

        Assert.assertEquals(1, permissionTOList.size());
        Assert.assertEquals("sensor:SID1:livedata:read", permissionTOList.get(0).getName());
    }

}
