package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.AbstractUUIDEntity;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @param <E> entity type
 * @param <T> transfer object type
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractUUIDDAO<E extends AbstractUUIDEntity, T> {

    protected final Class<E> entityClass;
    protected final Class<T> transferObjectClass;

    @PersistenceContext(unitName = "UrbanPulseManagement-PU")
    protected EntityManager entityManager;

    protected AbstractUUIDDAO(Class<E> entityClass, Class<T> transferObjectClass) {
        this.entityClass = entityClass;
        this.transferObjectClass = transferObjectClass;
    }

    // To implement this, use the transfer object's constructor with entity as parameter
    protected T toTransferObject(E entity) {
        try {
            return transferObjectClass.getDeclaredConstructor(entityClass).newInstance(entity);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(AbstractUUIDDAO.class.getName()).log(Level.SEVERE, "Cannot instantiate transfer object", ex);
            return null;
        }
    }

    protected List<T> toTransferObjectList(List<E> entities) {
        return entities.stream().map(this::toTransferObject).collect(Collectors.toList());
    }

    public T getById(String id) {
        E entity = queryById(id);
        if (null == entity) {
            return null;
        }

        return toTransferObject(entity);
    }

    public E queryById(String id) {
        return entityManager.find(entityClass, id);
    }

    public List<T> getAll() {
        List<E> entities = queryAll();
        return toTransferObjectList(entities);
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
        List<E> entities = AbstractUUIDDAO.this.queryFilteredBy(columnName, value);
        return toTransferObjectList(entities);
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
        return toTransferObjectList(entities);
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
