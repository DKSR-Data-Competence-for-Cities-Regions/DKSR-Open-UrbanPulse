package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.modules.AuthMethods;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import javax.inject.Inject;

/**
 * wrapper around {@link UpdateListenerDAO}
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@LocalBean
@Stateless
public class OutboundInterfacesManagementDAO {
    @Inject
    private UpdateListenerDAO updateListenerDao;


    public UpdateListenerTO createUpdateListener(StatementEntity statement, String target, AuthJsonTO authJson) {
        return updateListenerDao.create(statement, target, authJson);
    }

    public UpdateListenerTO getUpdateListenerById(String listenerId) {
        return updateListenerDao.getById(listenerId);
    }

    public String deleteUpdateListener(String listenerId) {
        return updateListenerDao.deleteById(listenerId);
    }

    public List<UpdateListenerTO> getUpdateListenersOfStatement(String statementId) {
        return updateListenerDao.getListenersOfStatement(statementId);
    }

    public int deleteAll() {
        return updateListenerDao.deleteAll();
    }

    public boolean isValidAuthMethod(AuthJsonTO authJson) {
        return (authJson == null ||
                authJson.getAuthMethod() == null ||
                AuthMethods.BASIC.name().equals(authJson.getAuthMethod())
                );
    }
}
