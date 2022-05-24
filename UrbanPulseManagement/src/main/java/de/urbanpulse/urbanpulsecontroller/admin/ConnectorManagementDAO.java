package de.urbanpulse.urbanpulsecontroller.admin;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.naming.OperationNotSupportedException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.urbanpulse.dist.jee.upsecurityrealm.hmac.Hasher.generateRandomHmacSha256Key;
import java.net.URI;
import static javax.ejb.TransactionAttributeType.MANDATORY;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@TransactionAttribute(MANDATORY)
@Stateless
@LocalBean
public class ConnectorManagementDAO extends AbstractUUIDDAO<ConnectorEntity, ConnectorTO> {

    public ConnectorManagementDAO() {
        super(ConnectorEntity.class, ConnectorTO.class);
    }

    /**
     *
     * @param description the description for the connector to create
     * @return the newly created connector
     */


    public ConnectorTO createConnector(String description) {
        return createConnector(description, null);
    }

    public ConnectorTO createConnector(String description, String hmacKey) {
        try {
            ConnectorEntity connector = new ConnectorEntity();
            connector.setDescription(description);
            connector.setKey((hmacKey == null) ? generateRandomHmacSha256Key() : hmacKey);

            entityManager.persist(connector);
            entityManager.flush();

            return toTransferObject(connector);
        } catch (NoSuchAlgorithmException | RuntimeException ex) {
            Logger.getLogger(ConnectorManagementDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }



    /**
     *
     * @param id of the connector to update
     * @param description the new description for the connector to update
     * @return the id of the updated connector or null if the connector could not be updated
     * @throws javax.naming.OperationNotSupportedException connector does not exist
     */
    public ConnectorTO updateConnector(String id, String description) throws OperationNotSupportedException {
        try {
            ConnectorEntity existingConnector = queryById(id);
            if (existingConnector == null) {
                throw new OperationNotSupportedException("connector to update does not exist");
            }

            existingConnector.setDescription(description);

            ConnectorEntity mergedConnector = entityManager.merge(existingConnector);
            entityManager.flush();

            if (mergedConnector != null) {
                return toTransferObject(mergedConnector);
            }

        } catch ( RuntimeException ex) {
            Logger.getLogger(ConnectorManagementDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
