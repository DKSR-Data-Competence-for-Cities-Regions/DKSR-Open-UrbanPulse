package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import javax.naming.OperationNotSupportedException;
import javax.persistence.Query;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class CategoryManagementDAO extends AbstractUUIDDAO<CategoryEntity, CategoryTO> {

    public CategoryManagementDAO() {
        super(CategoryEntity.class, CategoryTO.class);
    }

    /**
     * creates a new category
     *
     * @param name the new category name
     * @param description the description of the new category
     * @param childCategoryIds all child category IDs of this category
     * @return the newly created Category
     * @throws de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException a non-existing child category is referenced
     * @throws de.urbanpulse.urbanpulsecontroller.admin.CyclicDependencyException a cyclic dependency is created
     */
    public CategoryTO createCategory(String name, String description, List<String> childCategoryIds)
            throws ReferencedEntityMissingException, CyclicDependencyException {

        CategoryEntity entity = new CategoryEntity();
        entity.setName(name);
        entity.setDescription(description);
        List<CategoryEntity> children = new LinkedList<>();

        for (String childId : childCategoryIds) {
            CategoryEntity child = queryById(childId);
            if (child == null) {
                throw new ReferencedEntityMissingException(
                        "category named[" + name + "] references missing child with ID[" + childId + "]");
            }

            if (wouldCreateCyclicDependency(entity, child)) {
                throw new CyclicDependencyException("The child named[" + child.getName()
                        + "] could not be added as it would create a cyclic dependency in the tree!");
            }

            CategoryEntity oldParent = child.getParentCategory();
            if (oldParent != null) {
                oldParent.removeChild(child);
                entityManager.persist(oldParent);
            }

            child.setParentCategory(entity);
            children.add(child);
            entityManager.persist(child);
        }

        entity.setChildCategories(children);
        entityManager.persist(entity);
        entityManager.flush();

        return toTransferObject(entity);
    }

    /**
     * updates an already existing category
     *
     * @param categoryToUpdate the category to update
     * @param name the name of the
     * @param description the description of the Category
     * @param childCategoryIds all child category IDs to ad as children
     * @throws de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException a non-existing child category is referenced
     * @throws de.urbanpulse.urbanpulsecontroller.admin.CyclicDependencyException a cyclic dependency is created
     * @throws javax.naming.OperationNotSupportedException category to update does not existd
     */
    public void updateCategory(CategoryTO categoryToUpdate, String name, String description, List<String> childCategoryIds)
            throws ReferencedEntityMissingException, CyclicDependencyException, OperationNotSupportedException {

        boolean isModified = false;

        CategoryEntity entity = queryById(categoryToUpdate.getId());
        if (entity == null) {
            throw new OperationNotSupportedException("category to update does not exist");
        }

        isModified = updateName(name, entity, isModified);

        isModified = updateDescription(description, entity, isModified);

        isModified = updateChildCategories(childCategoryIds, categoryToUpdate, entity, isModified);

        if (isModified) {
            entityManager.persist(entity);
            entityManager.flush();
        }
    }

    private boolean updateChildCategories(List<String> childCategoryIds, CategoryTO categoryToUpdate, CategoryEntity entity, boolean isModified) throws ReferencedEntityMissingException, CyclicDependencyException {
        if (childCategoryIds != null && !childCategoryIds.isEmpty()) {
            for (String childId : childCategoryIds) {
                CategoryEntity child = queryById(childId);
                if (child == null) {
                    throw new ReferencedEntityMissingException(
                            "category named[" + categoryToUpdate.getName() + "] references missing child with ID[" + childId + "]");
                }

                if (wouldCreateCyclicDependency(entity, child)) {
                    throw new CyclicDependencyException("The child named[" + child.getName()
                            + "] could not be added as it would create a cyclic dependency in the tree!");
                }

                CategoryEntity oldParent = child.getParentCategory();
                if (oldParent != null) {
                    oldParent.removeChild(child);
                    entityManager.persist(oldParent);
                }

                child.setParentCategory(entity);
                entity.addChild(child);
                entityManager.persist(child);
                isModified = true;
            }
        }
        return isModified;
    }

    private boolean updateDescription(String description, CategoryEntity entity, boolean isModified) {
        if (description != null && !description.isEmpty()) {
            entity.setDescription(description);
            isModified = true;
        }
        return isModified;
    }

    private boolean updateName(String name, CategoryEntity entity, boolean isModified) {
        if (name != null && !name.isEmpty()) {
            entity.setName(name);
            isModified = true;
        }
        return isModified;
    }

    /**
     *
     * @param parentTO the parent for which the children should be queried
     * @return all child categories
     */
    public List<CategoryTO> getChildCategories(CategoryTO parentTO) {
        List<CategoryTO> children = new LinkedList<>();

        CategoryEntity parent = queryById(parentTO.getId());
        if (parent == null) {
            return children;
        }

        for (CategoryEntity child : parent.getChildCategories()) {
            CategoryTO childTO = toTransferObject(child);
            children.add(childTO);
        }

        return children;
    }

    /**
     *
     * @param parentTO the parent for which the children should be queried
     * @return all child categories fully resolved, so its is a complete category tree beginning from the parent node.
     */
    public List<CategoryWithChildrenTO> getFullyResolvedChildCategories(CategoryTO parentTO) {
        List<CategoryWithChildrenTO> children = new LinkedList<>();

        CategoryEntity parent = queryById(parentTO.getId());

        for (CategoryEntity child : parent.getChildCategories()) {
            CategoryWithChildrenTO childTO = new CategoryWithChildrenTO(child);
            children.add(childTO);
        }

        return children;
    }

    /**
     * Retrieve all root categories, these are all categories without any parent
     *
     * @return all root categories (categories without any parent)
     */
    public List<CategoryTO> getRootCategories() {
        List<CategoryTO> categories = new LinkedList<>();

        Query allRootCategoriesQuery = entityManager.createNamedQuery(CategoryEntity.ALL_ROOT_CATEGORIES_QUERY);
        List<CategoryEntity> resultList = allRootCategoriesQuery.getResultList();

        for (CategoryEntity category : resultList) {
            CategoryTO rootTO = toTransferObject(category);
            categories.add(rootTO);
        }

        return categories;
    }

    /**
     * Retrieve all root categories, these are all categories without any parent
     *
     * @return all root categories (categories without any parent)
     */
    public List<CategoryEntity> getRootCategoriesEntities() {
        Query allRootCategoriesQuery = entityManager.createNamedQuery(CategoryEntity.ALL_ROOT_CATEGORIES_QUERY);
        return allRootCategoriesQuery.getResultList();
    }

    /**
     * Retrieve all root categories, these are all categories without any parent
     *
     * @return all root categories (categories without any parent)
     */
    public List<CategoryWithChildrenTO> getFullyResolvedRootCategories() {
        List<CategoryWithChildrenTO> categories = new LinkedList<>();

        Query allRootCategoriesQuery = entityManager.createNamedQuery(CategoryEntity.ALL_ROOT_CATEGORIES_QUERY);
        List<CategoryEntity> resultList = allRootCategoriesQuery.getResultList();

        for (CategoryEntity category : resultList) {
            CategoryWithChildrenTO rootTO = new CategoryWithChildrenTO(category);
            categories.add(rootTO);
        }

        return categories;
    }

    public CategoryWithChildrenTO resolveCategory(CategoryTO categoryTO) {
        CategoryWithChildrenTO resolvedCategory = null;

        CategoryEntity entity = queryById(categoryTO.getId());
        if (entity == null) {
            return resolvedCategory;
        }

        resolvedCategory = new CategoryWithChildrenTO(entity);

        return resolvedCategory;
    }

    /**
     * retrieves the parent category of a given category
     *
     * @param id the id of the category to get the parent for
     * @return the parent category
     */
    public CategoryTO getParentCategory(String id) {
        CategoryEntity entity = queryById(id);
        if (entity == null) {
            return null;
        }

        CategoryEntity parent = entity.getParentCategory();
        if (parent == null) {
            return null;
        }

        return toTransferObject(parent);
    }

    /**
     * Deletes a category and unsets the parent reference on all children if any
     *
     * @param id The Id of the category to delete
     * @return either null or the id fo the deleted CategoryEntity
     */
    public String deleteByIdWithDependencies(String id) {
        CategoryEntity entity = queryById(id);
        if (entity == null) {
            return null;
        }

        for (CategoryEntity child : entity.getChildCategories()) {
            child.setParentCategory(null);
            entityManager.persist(child);
        }

        CategoryEntity parent = entity.getParentCategory();
        if (parent != null) {
            parent.removeChild(entity);
            entityManager.persist(parent);
        }

        return deleteById(id);
    }

    /**
     * Retrieves all child sensors of a category and also recursively all the sensors of their children!
     *
     * @param id of the category to get the sensors for
     * @return all child sensors of a category and recursively all the sensors of their children or empty optional if category
     *  does not exist
     */
    public Optional<List<SensorTO>> getSensors(String id) {
        CategoryEntity entity = queryById(id);
        if (entity == null) {
            return Optional.empty();
        }

        final List<SensorEntity> sensors = getSensorsForAllChildrenInTree(entity);

        return Optional.of(sensors.stream().map(SensorTO::new).collect(Collectors.toList()));
    }

    private List<SensorEntity> getSensorsForAllChildrenInTree(CategoryEntity parentCategory) {
        final List<SensorEntity> sensorsOfChildsChildren = new LinkedList<>();

        for (CategoryEntity child : parentCategory.getChildCategories()) {
            sensorsOfChildsChildren.addAll(getSensorsForAllChildrenInTree(child));
        }

        List<SensorEntity> childSensors = parentCategory.getSensors();

        childSensors.addAll(sensorsOfChildsChildren);
        return childSensors;
    }

    private boolean wouldCreateCyclicDependency(CategoryEntity parent, CategoryEntity child) {
        boolean retVal = false;

        CategoryEntity grandpa = parent.getParentCategory();

        if (grandpa != null) {
            if (grandpa.equals(child)) {
                return true;
            }
            retVal = wouldCreateCyclicDependency(grandpa, child);
        }

        return retVal;
    }

    @Override
    public String deleteById(String id) {
        CategoryEntity entity = queryById(id);
        if (null == entity) {
            return null;
        }

        removeFromSensors(entity);

        entityManager.remove(entity);
        entityManager.flush();
        return id;
    }

    private void removeFromSensors(CategoryEntity entity) {
        List<SensorEntity> sensors = entity.getSensors();
        for (SensorEntity sensor : sensors) {
            sensor.removeCategory(entity);
            entityManager.merge(sensor);
        }
    }
}
