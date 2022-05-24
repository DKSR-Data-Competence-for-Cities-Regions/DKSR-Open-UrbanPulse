package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.PermissionTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class RolesManagementDAOTest {

    private static final Class ROLE_ENTITY_CLASS = RoleEntity.class;
    private static final long ROLE_ID = 4711;
    private static final String ROLE_ID_STRING = Long.toString(ROLE_ID);
    private static final PermissionTO MY_PERMISSION_TO = new PermissionTO(UUID.randomUUID().toString(), "myPermission");
    private static final PermissionTO MY_OTHER_PERMISSION_TO = new PermissionTO(UUID.randomUUID().toString(), "myOtherPermission");
    private static final PermissionEntity MY_PERMISSION_ENTITY = new PermissionEntity(UUID.randomUUID().toString(), "myPermission");
    private static final PermissionEntity MY_OTHER_PERMISSION_ENTITY = new PermissionEntity(UUID.randomUUID().toString(), "myOtherPermission");

    @Mock
    protected EntityManager entityManager;
    @Mock
    RoleEntity roleMock;
    @Mock
    RoleTO roleTOMock;
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
    private RoleManagementDAO roleDao;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateRole_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);
        given(entityManager.find(PermissionEntity.class, MY_PERMISSION_TO.getId())).willReturn(MY_PERMISSION_ENTITY);
        given(entityManager.find(PermissionEntity.class, MY_OTHER_PERMISSION_TO.getId())).willReturn(MY_OTHER_PERMISSION_ENTITY);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(ROLE_ENTITY_CLASS)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);
        given(queryMock.getResultList()).willReturn(new ArrayList<>());

        RoleTO roleTO = new RoleTO("gee", "Role, role, role your boat gently down the stream", Arrays.asList(MY_PERMISSION_TO, MY_OTHER_PERMISSION_TO));
        RoleTO returnedRoleTO = roleDao.createRole(roleTO);

        verify(entityManager).persist(any(RoleEntity.class));
        verify(entityManager).flush();

        assertNotNull(returnedRoleTO);
    }

    @Test
    public void testGetAll_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(ROLE_ENTITY_CLASS)).willReturn(rootMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);

        List<RoleEntity> existingRoles = new ArrayList<>();
        existingRoles.add(roleMock);
        given(queryMock.getResultList()).willReturn(existingRoles);

        List<RoleTO> returnedRoles = roleDao.getAll();

        assertNotNull(returnedRoles);
        assertEquals(1, returnedRoles.size());
    }

    @Test
    public void testDeleteById_returnsExpected() throws Exception {
        given(entityManager.find(RoleEntity.class, ROLE_ID_STRING)).willReturn(roleMock);

        String deletedRoleId = roleDao.deleteById(ROLE_ID_STRING);

        verify(entityManager, times(1)).remove(roleMock);

        assertEquals(ROLE_ID_STRING, deletedRoleId);
    }

    @Test
    public void testDeleteById_noRoleFound() throws Exception {
        String deletedRoleId = roleDao.deleteById(ROLE_ID_STRING);

        verify(entityManager, never()).remove(roleMock);

        assertNull(deletedRoleId);
    }

}
