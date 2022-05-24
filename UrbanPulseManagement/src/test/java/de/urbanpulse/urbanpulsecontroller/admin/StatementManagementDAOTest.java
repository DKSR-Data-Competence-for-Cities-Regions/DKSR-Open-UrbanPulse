/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class StatementManagementDAOTest {

    @Mock
    protected EntityManager entityManager;

    @InjectMocks
    StatementManagementDAO statementManagementDAO;

    @Mock
    StatementEntity statementMock;

    @Mock
    StatementTO statementTOMock;

    @Mock
    private CriteriaBuilder criteriaBuilderMock;

    @Mock
    CriteriaQuery criteriaQueryMock;

    @Mock
    TypedQuery queryMock;

    @Mock
    Root rootMock;

    @Mock
    Path columnPathMock;

    public StatementManagementDAOTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateStatement_WithNoStatementFound() throws Exception {

        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(StatementEntity.class)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);
        given(queryMock.getResultList()).willReturn(new ArrayList<>());

        StatementTO createdStatement = statementManagementDAO.createStatement("myStatement", "Select from 'foo' where 'bar'=1");

        verify(entityManager).persist(any(StatementEntity.class));
        verify(entityManager).flush();

        Assert.assertNotNull(createdStatement);
        Assert.assertEquals("myStatement", createdStatement.getName());
        Assert.assertEquals("Select from 'foo' where 'bar'=1", createdStatement.getQuery());
    }

    @Test
    public void testCreateStatement_WithExistingStatementFound() throws Exception {

        given(entityManager.getCriteriaBuilder()).willReturn(criteriaBuilderMock);
        given(entityManager.createQuery(criteriaQueryMock)).willReturn(queryMock);

        given(criteriaBuilderMock.createQuery()).willReturn(criteriaQueryMock);
        given(criteriaQueryMock.from(StatementEntity.class)).willReturn(rootMock);
        given(rootMock.get(anyString())).willReturn(columnPathMock);

        given(criteriaQueryMock.select(rootMock)).willReturn(criteriaQueryMock);

        List<StatementEntity> existingStatements = new ArrayList<>();
        existingStatements.add(statementMock);
        given(queryMock.getResultList()).willReturn(existingStatements);

        StatementTO createdStatement = statementManagementDAO.createStatement("myStatement", "Select from 'foo' where 'bar'=1");

        verify(entityManager, never()).persist(any(StatementEntity.class));
        verify(entityManager, never()).flush();

        Assert.assertNull(createdStatement);
    }
}
