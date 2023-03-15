package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.RoleTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UserTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UserManagementDAOTest {

    private static final Class USER_ENTITY_CLASS = UserEntity.class;
    private static final long USER_ID = 4711;
    private static final String USER_ID_STRING = Long.toString(USER_ID);
    private static final RoleTO MY_ROLE_TO = new RoleTO(UUID.randomUUID().toString(), "myRole");
    private static final RoleTO MY_OTHER_ROLE_TO = new RoleTO(UUID.randomUUID().toString(), "myOtherRole");
    private static final RoleEntity MY_ROLE_ENTITY = new RoleEntity(UUID.randomUUID().toString(), "myRole");
    private static final RoleEntity MY_OTHER_ROLE_ENTITY = new RoleEntity(UUID.randomUUID().toString(), "myOtherRole");

    @Mock
    protected EntityManager entityManager;
    @Mock
    UserEntity userMock;
    @Mock
    UserTO userTOMock;
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
    private UserManagementDAO userDao;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateUser_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);
        given(entityManager.find(RoleEntity.class, MY_ROLE_TO.getId())).willReturn(MY_ROLE_ENTITY);
        given(entityManager.find(RoleEntity.class, MY_OTHER_ROLE_TO.getId())).willReturn(MY_OTHER_ROLE_ENTITY);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(USER_ENTITY_CLASS)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);
        given(queryMock.getResultList()).willReturn(new ArrayList<>());

        UserTO userTO = new UserTO("gee", "superSecret", "aSecretKey", Arrays.asList(MY_ROLE_TO, MY_OTHER_ROLE_TO), Collections.emptyList());
        UserTO returnedUserTO = userDao.createUser(userTO);

        verify(entityManager).persist(any(UserEntity.class));
        verify(entityManager).flush();

        assertNotNull(returnedUserTO);
    }

    @Test
    public void testCreateUser_NameTaken() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(USER_ENTITY_CLASS)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);

        List<UserEntity> existingUsers = new ArrayList<>();
        existingUsers.add(userMock);
        given(queryMock.getResultList()).willReturn(existingUsers);

        UserTO userTO = new UserTO("gee", "superSecret", "aSecretKey", Arrays.asList(MY_ROLE_TO, MY_OTHER_ROLE_TO), Collections.emptyList());
        UserTO returnedUserTO = userDao.createUser(userTO);

        verify(entityManager, never()).persist(any(UserEntity.class));
        verify(entityManager, never()).flush();

        assertNull(returnedUserTO);
    }

    @Test
    public void testGetAll_returnsExpected() throws Exception {
        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(USER_ENTITY_CLASS)).willReturn(rootMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);

        List<UserEntity> existingUsers = new ArrayList<>();
        existingUsers.add(userMock);
        given(queryMock.getResultList()).willReturn(existingUsers);

        List<UserTO> returnedUsers = userDao.getAll();

        assertNotNull(returnedUsers);
        assertEquals(1, returnedUsers.size());
    }

    @Test
    public void testDeleteById_returnsExpected() throws Exception {
        given(entityManager.find(UserEntity.class, USER_ID_STRING)).willReturn(userMock);

        String deletedUserId = userDao.deleteById(USER_ID_STRING);

        verify(entityManager, times(1)).remove(userMock);

        assertEquals(USER_ID_STRING, deletedUserId);
    }

    @Test
    public void testDeleteById_noUserFound() throws Exception {
        given(entityManager.find(UserEntity.class, USER_ID_STRING)).willReturn(null);

        String deletedUserId = userDao.deleteById(USER_ID_STRING);

        verify(entityManager, never()).remove(userMock);

        assertNull(deletedUserId);
    }

    @Test
    public void update_returnsExpectedAfterUpdatingPassword() throws Exception {
        given(entityManager.find(UserEntity.class, USER_ID_STRING)).willReturn(userMock);
        given(userMock.getId()).willReturn(USER_ID_STRING);

        UserTO userTO = userDao.getById(USER_ID_STRING);
        userTO.setPassword("newPassword");

        UserTO returnedUserTO = userDao.update(userTO);

        verify(userMock).setPasswordHash(anyString());
        verify(entityManager).merge(any(UserEntity.class));
        verify(entityManager).flush();

        assertEquals(USER_ID_STRING, returnedUserTO.getId());
    }

    @Test
    public void changePassword_returnsExpected() {
        given(entityManager.find(UserEntity.class, USER_ID_STRING)).willReturn(userMock);
        given(userMock.getId()).willReturn(USER_ID_STRING);

        final UserTO userTO = userDao.changePassword(USER_ID_STRING, "newPassword");

        verify(userMock).setPasswordHash(anyString());
        verify(entityManager).merge(any(UserEntity.class));
        verify(entityManager).flush();

        assertEquals(USER_ID_STRING, userTO.getId());
    }
}
