package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.UpdateListenerEntity;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.Hasher;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@LocalBean
@Stateless
public class UpdateListenerDAO extends AbstractUUIDDAO<UpdateListenerEntity, UpdateListenerTO> {

    public UpdateListenerDAO() {
        super(UpdateListenerEntity.class, UpdateListenerTO.class);
    }

    /**
     *
     * @param statement
     * @param target
     * @return UpdateListenerTO
     * @deprecated
     */
    @Deprecated
    public UpdateListenerTO create(StatementEntity statement, String target) {
        try {
            UpdateListenerEntity entity = new UpdateListenerEntity();
            entity.setTarget(target);
            entity.setStatement(statement);

            entity.setKey(Hasher.generateRandomHmacSha256Key());
            entityManager.persist(entity);
            entityManager.flush();
            return toTransferObject(entity);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public UpdateListenerTO create(StatementEntity statement, String target, AuthJsonTO authJson) {
        try {
            UpdateListenerEntity entity = new UpdateListenerEntity();
            entity.setTarget(target);
            entity.setStatement(statement);
            if (authJson == null) {
                entity.setAuthJson(new AuthJsonTO().toString());
            } else {
                entity.setAuthJson(authJson.toString());
            }

            entity.setKey(Hasher.generateRandomHmacSha256Key());
            entityManager.persist(entity);
            entityManager.flush();
            return toTransferObject(entity);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int deleteAll() {
        int count = entityManager.createNativeQuery("DELETE FROM up_update_listeners").executeUpdate();
        entityManager.flush();
        return count;
    }

    public List<UpdateListenerTO> getListenersOfStatement(String statementId) {
        StatementEntity entity = entityManager.find(StatementEntity.class, statementId);
        if (entity == null) {
            return new ArrayList<>();
        }
        List<UpdateListenerEntity> updateListeners = entity.getUpdateListeners();
        return toTransferObjectList(updateListeners);
    }
}
