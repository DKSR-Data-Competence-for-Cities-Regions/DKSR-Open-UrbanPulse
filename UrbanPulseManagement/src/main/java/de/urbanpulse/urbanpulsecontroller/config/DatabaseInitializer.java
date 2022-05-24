package de.urbanpulse.urbanpulsecontroller.config;

import de.urbanpulse.dist.jee.entities.PermissionEntity;
import de.urbanpulse.dist.jee.entities.RoleEntity;
import de.urbanpulse.dist.jee.entities.UserEntity;
import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.OutboundInterfacesManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.InboundSetupDAO;
import de.urbanpulse.urbanpulsecontroller.admin.modules.PersistenceV3SetupDAO;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultPermissions.*;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.*;

import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ensures a certain state of the database after startup
 * it is ESSENTIAL to create a new admin user and then remove the default one
 * using credentials of the new admin, as the default credentials are HIGHLY
 * INSECURE!
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class DatabaseInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class.getName());

    @PersistenceContext(unitName = "UrbanPulseManagement-PU")
    private EntityManager entityManager;

    @Inject
    private UserManagementDAO userManagementDao;

    @Inject
    private CategoryManagementDAO categoryDao;

    @Inject
    private InboundSetupDAO inboundDao;

    @Inject
    private PersistenceV3SetupDAO persistenceV3Dao;



    @Inject
    private OutboundInterfacesManagementDAO outboundInterfacesDao;

    @Resource
    private EJBContext context;

    public void initializeDb() {

        UserTransaction tx = context.getUserTransaction();

        try {
            tx.begin();
            checkRole(ADMIN);

            createPermissionIfNotExists(HISTORIC_DATA_OPERATOR_FOR_ALL_SENSORS);
            createPermissionIfNotExists(HISTORIC_DATA_READER_FOR_ALL_SENSORS);
            createPermissionIfNotExists(PERMISSION_OPERATOR_FOR_ALL_SENSORS);
            createPermissionIfNotExists(LIVE_DATA_READER_FOR_ALL_SENSORS);


            initAdminRolePermissions();


            if (userManagementDao.getAdmins().isEmpty()) {
                LOG.info("No admins found in the database, insert insecure default admin...");

                createInsecureDefaultAdmin();
            } else {
                LOG.info("An admin account has been found in the database, not seeding the default admin.");
            }

            if (!persistenceV3Dao.hasConfig()) {
                String path = System.getProperty("com.sun.aas.instanceRoot") + "/config/sql/persistence.sql";
                LOG.log(Level.INFO, "Persistence V3 setup table is empty, try seeding from generated chef generated sql file ({0})",
                        path);
                executeSql(path);
                LOG.fine("Done seeding persistence v3 table");
            } else {
                LOG.info("Persistence V3 setup table is not empty, not seeding the table.");
            }



            if (!inboundDao.hasConfig()) {
                String path = System.getProperty("com.sun.aas.instanceRoot") + "/config/sql/inbound.sql";
                LOG.log(Level.INFO, "Inbound setup table is empty, try seeding from generated chef generated sql file ({0})", path);
                executeSql(path);
                LOG.fine("Done seeding inbound setup table");
            } else {
                LOG.info("Inbound setup table is not empty, not seeding the table.");
            }

            tx.commit();
        } catch (Exception e) {
            try {
                LOG.log(Level.WARNING, "Doing rollback");
                tx.rollback();
                LOG.log(Level.INFO, "Rollback done");
            } catch (SecurityException | SystemException ie) {
                throw new IllegalStateException(ie);
            }
            throw new IllegalStateException("Exception while running DatabaseInitializer", e);
        }
    }

    private void initAdminRolePermissions() {
        List<String> permissionsToAdminRole = new ArrayList<>();
        permissionsToAdminRole.add(HISTORIC_DATA_OPERATOR_FOR_ALL_SENSORS);
        permissionsToAdminRole.add(LIVE_DATA_READER_FOR_ALL_SENSORS);
        permissionsToAdminRole.add(PERMISSION_OPERATOR_FOR_ALL_SENSORS);

        addPermissionsToRole(permissionsToAdminRole, ADMIN);
    }



    void executeSql(String path) {
        try {
            if (!Paths.get(path).toFile().exists()) {
                LOG.log(Level.WARNING, "SQL script file does not exist: {0}", path);
                return;
            }
            String sql = new String(Files.readAllBytes(Paths.get(path)));

            String[] statements = sql.split(";\n");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) { // ignore empty lines within the file to prevent exceptions
                    LOG.log(Level.INFO, "Running query: {0}", statement);
                    entityManager.createNativeQuery(statement).executeUpdate();
                }
            }
        } catch (IOException e) {
            String msg = "Error reading sql script from file " + path;
            LOG.log(Level.SEVERE, msg, e);
        }
    }

    void createInsecureDefaultAdmin() {
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                " !!! creating INSECURE default admin user, REPLACE it with one that uses SECURE credentials ASAP !!! ");
        UserEntity user = new UserEntity();
        user.setName("admin");
        user.setKey("key");
        List<RoleEntity> roles = entityManager.createNamedQuery("getRoleByName", RoleEntity.class)
                .setParameter("roleName", UPDefaultRoles.ADMIN).getResultList();

        String passwordHash = BCrypt.hashpw("password", BCrypt.gensalt());
        user.setPasswordHash(passwordHash);
        entityManager.persist(user);
        if (!roles.isEmpty()) {
            RoleEntity role = roles.get(0);
            user.getRoles().add(role);
            role.getUsers().add(user);
        }
        entityManager.flush();
    }

    private void checkRole(String role) {
        if (entityManager.createNamedQuery("getRoleByName", RoleEntity.class).setParameter("roleName", role).getResultList()
                .isEmpty()) {
            LOG.log(Level.INFO, "No {0} role found in the database, inserting missing role...", role);
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setName(role);
            entityManager.persist(roleEntity);
        }
    }

    private void createPermissionIfNotExists(String permissionName) {
        if (entityManager.createNamedQuery("getPermissionByName", PermissionEntity.class).setParameter("permissionName", permissionName).getResultList().isEmpty()) {
            LOG.log(Level.INFO, "No {0} permission found in the database, inserting missing permission...", permissionName);
            PermissionEntity permissionEntity = new PermissionEntity();
            permissionEntity.setName(permissionName);
            entityManager.persist(permissionEntity);
        }
    }

    private void addPermissionsToRole(List<String> permissionNames, String roleName) {
        List<PermissionEntity> requiredPermissionEntities = new ArrayList<>();
        permissionNames.forEach(permissionName -> requiredPermissionEntities
                .addAll(entityManager.createNamedQuery("getPermissionByName", PermissionEntity.class)
                        .setParameter("permissionName", permissionName)
                        .getResultList()));
        List<RoleEntity> roleEntities = entityManager.createNamedQuery("getRoleByName", RoleEntity.class).setParameter("roleName", roleName).getResultList();
        if (roleEntities.size() == 1) {
            RoleEntity roleEntity = roleEntities.get(0);
            roleEntity.getPermissions().forEach(permissionEntity -> {
                if (!requiredPermissionEntities.contains(permissionEntity)) {
                    requiredPermissionEntities.add(permissionEntity);
                }
            });

            roleEntity.setPermissions(requiredPermissionEntities);

            entityManager.merge(roleEntity);
            entityManager.flush();
        }
    }

}
