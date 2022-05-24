package de.urbanpulse.urbanpulsecontroller.admin.modules;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * DAO base class
 *
 * always assumes String as ID type, always returns (collection of) entities
 *
 * @param <E> entity type
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AbstractStringIdDAO<E> {

    protected final Class<E> entityClass;

    @PersistenceContext(unitName = "UrbanPulseManagement-PU")
    protected EntityManager entityManager;

    protected AbstractStringIdDAO(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    public E queryById(String id, LockModeType lockMode) {
        return entityManager.find(entityClass, id, lockMode);
    }

    public E queryById(String id) {
        return entityManager.find(entityClass, id);
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

    /**
     * delete all entities
     */
    public void deleteAll() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass);
        Query query = entityManager.createQuery(delete);
        query.executeUpdate();
        entityManager.flush();
    }

    /**
     * delete all rows where the column matches the given value
     *
     * @param column the column that should match
     * @param value value of the column
     */
    public void deleteFilteredBy(String column, Object value) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(entityClass);
        final Path<E> columnPath = root.get(column);
        Predicate columnEqualsPredicate = builder.equal(columnPath, value);

        CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass).where(columnEqualsPredicate);
        Query query = entityManager.createQuery(delete);
        query.executeUpdate();
        entityManager.flush();
    }

    /**
     * updates the database from the entity
     *
     * @param entity the entity to be merged
     */
    public void merge(E entity) {
        entityManager.merge(entity);
    }

    /**
     * resets the entity to the state from the database
     *
     * @param entity the entity to be refreshed
     */
    public void refresh(E entity) {
        entityManager.refresh(entity);
    }

    public List<E> queryFilteredBy(String column, Object value) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = builder.createQuery();
        Root<E> root = criteriaQuery.from(entityClass);
        final Path<E> columnPath = root.get(column);
        Predicate columnEqualsPredicate = builder.equal(columnPath, value);

        criteriaQuery.where(columnEqualsPredicate);

        criteriaQuery.select(root);
        Query query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
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
