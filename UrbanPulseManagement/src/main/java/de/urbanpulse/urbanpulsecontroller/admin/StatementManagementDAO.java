package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
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
@Stateless
@LocalBean
public class StatementManagementDAO extends AbstractUUIDDAO<StatementEntity, StatementTO> {

    public StatementManagementDAO() {
        super(StatementEntity.class, StatementTO.class);
    }

    /**
     * persist a new statement
     *
     * @param name statement name
     * @param query query definition
     * @return statement if created, none if one with name already exists
     */
    public StatementTO createStatement(String name, String query) {
        return createStatement(name, query, null);
    }

    /**
     * persist a new statement
     *
     * @param name statement name
     * @param query query definition
     * @param comment An optional comment describing the statement
     * @return statement if created, none if one with name already exists
     */
    public StatementTO createStatement(String name, String query, String comment) {
        final boolean noStatementWithNameFound = queryFilteredBy("name", name).isEmpty();
        if (noStatementWithNameFound) {
            StatementEntity entity = new StatementEntity();
            entity.setName(name);
            entity.setQuery(query);
            entity.setComment(comment);
            entityManager.persist(entity);
            entityManager.flush();
            return toTransferObject(entity);
        } else {
            return null;
        }
    }
}
