package de.urbanpulse.urbanpulsecontroller.admin;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Deprecated
public class AbstractDAO<E extends Serializable, T extends Serializable> {

    protected final Class<E> entityClass;
    protected final Class<T> transferObjectClass;

    protected TransferObjectFactory transferObjectFactory = new TransferObjectFactory();

    @PersistenceContext(unitName = "UrbanPulseManagement-PU")
    protected EntityManager entityManager;

    protected AbstractDAO(Class<E> entityClass, Class<T> transferObjectClass) {
        this.entityClass = entityClass;
        this.transferObjectClass = transferObjectClass;
    }

    public T getById(String id) {
        E entity = queryById(id);
        if (null == entity) {
            return null;
        }

        return transferObjectFactory.create(entity, entityClass, transferObjectClass);
    }

    public E queryById(String id) {
        E result = null;
        try {
            long numericId = Long.parseLong(id);
            result = entityManager.find(entityClass, numericId);
        } catch (NumberFormatException e) {
            Logger.getLogger(AbstractDAO.class.getName()).log(Level.SEVERE, null, e);
        }
        return result;
    }

    public List<T> getAll() {
        List<E> entities = queryAll();
        return transferObjectFactory.createList(entities, entityClass, transferObjectClass);
    }

    public List<E> queryAll() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(entityClass);
        criteriaQuery.select(root);

        Query query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public String deleteById(String id) {
        E entity = queryById(id);
        if (null == entity) {
            return null;
        }

        entityManager.remove(entity);
        entityManager.flush();
        return id;
    }

    public List<T> getFilteredBy(String columnName, Object value) {
        List<E> entities = AbstractDAO.this.queryFilteredBy(columnName, value);
        return transferObjectFactory.createList(entities, entityClass, transferObjectClass);
    }

    public List<E> queryFilteredBy(String column, Object value) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(entityClass);
        final Path<?> columnPath = root.get(column);
        Predicate columnEqualsPredicate = builder.equal(columnPath, value);

        criteriaQuery.where(columnEqualsPredicate);

        criteriaQuery.select(root);
        Query query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public List<T> getFilteredBy(Map<String, Object> columnNameValuePairs) {
        List<E> entities = queryFilteredBy(columnNameValuePairs);
        return transferObjectFactory.createList(entities, entityClass, transferObjectClass);
    }

    public List<E> queryFilteredBy(Map<String, Object> columnNameValuePairs) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(entityClass);

        Predicate andPredicate = buildAndEqualsPredicate(columnNameValuePairs, root, builder);
        criteriaQuery.where(andPredicate);

        criteriaQuery.select(root);
        Query query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    private Predicate buildAndEqualsPredicate(Map<String, Object> columnNameValuePairs, Root<E> root, CriteriaBuilder builder) {
        List<Predicate> predicates = new LinkedList<>();
        for (Map.Entry<String, Object> pair : columnNameValuePairs.entrySet()) {
            String columnName = pair.getKey();
            Object value = pair.getValue();
            final Path<?> columnPath = root.get(columnName);
            Predicate columnEqualsPredicate = builder.equal(columnPath, value);
            predicates.add(columnEqualsPredicate);
        }

        Predicate[] columnsEqualPredicates = predicates.toArray(new Predicate[predicates.size()]);
        return builder.and(columnsEqualPredicates);
    }
}
