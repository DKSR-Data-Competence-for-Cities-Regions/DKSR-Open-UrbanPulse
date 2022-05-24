package de.urbanpulse.dist.jee.upsecurityrealm;

import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A DAO to retrieve Users and Connectors
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class SecurityRealmDAO {

    @PersistenceContext(unitName = "UPSecurityRealmPU")
    private EntityManager em;

    public UserEntity getUserbyName(String name) {
        List<UserEntity> users = em.createNamedQuery("userByName", UserEntity.class).setParameter("name", name).getResultList();
        if (users == null || users.isEmpty()) {
            return null;
        } else {
            return users.get(0);
        }
    }

    public UserEntity getUserById(String id) {
        return em.find(UserEntity.class, id);
    }

    public ConnectorEntity getConnectorById(String connectorId) {
        try {
            return em.find(ConnectorEntity.class, connectorId);
        } catch (NumberFormatException e) {
            return null;
        }

    }

}
