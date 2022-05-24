package de.urbanpulse.persistence.v3.storage;

import java.sql.Connection;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import city.ui.shared.db.migrate.bean.AbstractMigrationBean;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class JPAMigrationBean extends AbstractMigrationBean {

    private static final String LOG_LOCK_TABLE_PREFIX = "lb_jpa_";
    private static final String MIGRATION_SCRIPT_SUB_FOLDER_NAME = "jpa";

    private final EntityManager entityManager;

    JPAMigrationBean(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doMigrate() {
        // Otherwise we can't unwrap the connection
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            super.doMigrate();
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    @Override
    protected Connection getConnection() {
        Connection connection = entityManager.unwrap(Connection.class);
        return connection;
    }

    @Override
    protected DataSource getDataSource() {
        throw new UnsupportedOperationException("getDataSource is not supported in JPAMigrationBean");
    }

    @Override
    protected String getLiquibaseTablePrefix() {
        return LOG_LOCK_TABLE_PREFIX;
    }

    @Override
    protected String getMigrationScriptSubFolderName() {
        return MIGRATION_SCRIPT_SUB_FOLDER_NAME;
    }
}
