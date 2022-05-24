package de.urbanpulse.urbanpulsecontroller.admin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * converts entities into transfer objects
 *
 * @deprecated See {@link AbstractUUIDDAO} on how to do it right
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@LocalBean
@Stateless
@Deprecated
public class TransferObjectFactory {

    /**
     * convert an entity into a transfer object by invoking the transfer object constructor that takes the entity as an argument and
     * this factory as a second argument to be able to create dependant transfer objects
     *
     * @param <T> transfer object type
     * @param <E> entity type
     * @param entity the entity
     * @param entityClass class of E
     * @param transferObjectClass class of T
     * @return list of transfer objects or null if reflection error occurred (logged)
     * @throws IllegalArgumentException any argument is null
     */
    public <T, E> T createWithDependencies(E entity, Class<E> entityClass, Class<T> transferObjectClass) {
        throwOnIllegalArguments(entity, "entity", entityClass, transferObjectClass);

        try {
            Constructor<T> constructor = transferObjectClass.getConstructor(entityClass, TransferObjectFactory.class);
            return constructor.newInstance(entity, this);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * convert an entity into a transfer object by invoking the transfer object constructor that takes the entity as sole argument
     *
     * @param <T> transfer object type
     * @param <E> entity type
     * @param entity the entity
     * @param entityClass class of E
     * @param transferObjectClass class of T
     * @return list of transfer objects or null if reflection error occurred (logged)
     * @throws IllegalArgumentException any argument is null
     */
    public <T, E> T create(E entity, Class<E> entityClass, Class<T> transferObjectClass) {
        throwOnIllegalArguments(entity, "entity", entityClass, transferObjectClass);

        try {
            Constructor<T> constructor = transferObjectClass.getConstructor(entityClass);
            return constructor.newInstance(entity);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private <T, E> void throwOnIllegalArguments(Object object, String objectName, Class<E> entityClass, Class<T> transferObjectClass) {
        if (object == null) {
            throw new IllegalArgumentException(objectName + " is null");
        }

        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass is null");
        }

        if (transferObjectClass == null) {
            throw new IllegalArgumentException("transferObjectClass is null");
        }
    }

    /**
     * convert a list of entities into a list of transfer objects by invoking the transfer object constructor that takes the entity
     * as sole argument
     *
     * @param <T> transfer object type
     * @param <E> entity type
     * @param entities list of entities
     * @param entityClass class of E
     * @param transferObjectClass class of T
     * @return list of transfer objects or null if reflection error occurred (logged)
     * @throws IllegalArgumentException any argument is null
     */
    public <T, E> List<T> createList(List<E> entities, Class<E> entityClass, Class<T> transferObjectClass) {
        throwOnIllegalArguments(entities, "entities", entityClass, transferObjectClass);

        try {
            List<T> transferObjects = new LinkedList<>();

            Constructor<T> constructor = transferObjectClass.getConstructor(entityClass);
            for (E entity : entities) {
                T transferObject = constructor.newInstance(entity);
                transferObjects.add(transferObject);
            }

            return transferObjects;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return Collections.emptyList();
        }
    }
}
