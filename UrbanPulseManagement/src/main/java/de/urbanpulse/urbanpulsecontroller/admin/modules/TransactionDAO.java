package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.TransactionEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPTransactionState;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * provides access to UP module command transactions in the database
 *
 * to avoid any state loss in our own transaction management we use a new database transaction for each DAO call, which will pause
 * the current transaction
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Stateless
public class TransactionDAO extends AbstractStringIdDAO<TransactionEntity> {

    public TransactionDAO() {
        super(TransactionEntity.class);
    }

    /**
     * always runs in a new transaction
     *
     * @param txId transaction ID (note: this is NOT the row ID!)
     * @param connectionId connection ID (registered module instance)
     * @return the new TransactionEntity
     */
    public TransactionEntity create(String txId, String connectionId) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTxId(txId);
        entity.setConnectionID(connectionId);
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    /**
     * always runs in a new transaction
     *
     * @param txId transaction ID (note: this is NOT the row ID!)
     */
    public void deleteByTxId(String txId) {
        deleteFilteredBy("txId", txId);
    }

    /**
     * always runs in a new transaction
     * @param entity the entity which state should be changed
     * @param state the state of the TransactionEntity
     */
    public void setTxState(TransactionEntity entity, UPTransactionState state) {
        entity.setTxState(state.name());
        entityManager.merge(entity);
        entityManager.flush();
    }
}
