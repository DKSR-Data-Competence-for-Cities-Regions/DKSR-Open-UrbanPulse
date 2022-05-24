package city.ui.shared.db.migrate.bean;

import city.ui.shared.db.migrate.exception.MigrationException;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public abstract class AbstractMigrationBean implements MigrationBean {

    private static final Logger LOG = Logger.getLogger(AbstractMigrationBean.class.getName());

    private static final String CHANGE_LOG_RESOURCE_FILE_NAME = "db.changelog.xml";
    private static final String DEFAULT_EXTERNAL_PATH = "c:/changeSet/";

    @Override
    public void doMigrate() {
        LOG.info("Migrating database...");

        String liquibaseTablePrefix = getLiquibaseTablePrefix();
        LOG.log(Level.INFO, "Prefix for Liquibase tables: {0}", liquibaseTablePrefix);

        try {
            Connection connection = getConnection();
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDatabaseChangeLogLockTableName(liquibaseTablePrefix + "changeLogLock");
            database.setDatabaseChangeLogTableName(liquibaseTablePrefix + "changeLog");

             // SQ will tell you to wrap this in a try-with-resources. Don't be fooled. If you do, migration will fail.
            @SuppressWarnings("java:S2095")
            Liquibase liquibase = new Liquibase(CHANGE_LOG_RESOURCE_FILE_NAME,
                    new CompositeResourceAccessor(new ClassLoaderResourceAccessor(), new FileSystemResourceAccessor()),
                    database);
            liquibase.setChangeLogParameter("preUpdateFile", getMigrationScriptPath(Phase.PRE_UPDATE));
            liquibase.setChangeLogParameter("postUpdateFile", getMigrationScriptPath(Phase.POST_UPDATE));
            liquibase.update((String) null);
            LOG.info("Migration is successful");
        } catch (SQLException | LiquibaseException e) {
            LOG.log(Level.SEVERE, "Exception while migrating database");
            throw new MigrationException("Exception while migrating database", e);
        }
    }

    private String getMigrationScriptPath(Phase phase) {
        String pathname = String.valueOf(Paths.get(DEFAULT_EXTERNAL_PATH, getMigrationScriptSubFolderName(), phase.getExternalFile()));
        File f = new File(pathname);
        if (f.exists() && !f.isDirectory()) {
            LOG.log(Level.WARNING, "External file found for migration: {0}", pathname);
            return pathname;
        }
        String defaultFileRelativePath = File.separator + phase.getExternalFile();
        LOG.log(Level.INFO, "No external file found, returning default from classpath: {0}", defaultFileRelativePath);
        return defaultFileRelativePath;
    }

    protected Connection getConnection() throws SQLException {
        DataSource dataSource = getDataSource();
        if (dataSource == null) {
            LOG.severe("JNDI lookup failed for data source");
            throw new MigrationException("JNDI lookup failed for data source");
        } else {
            return dataSource.getConnection();
        }
    }

    protected abstract DataSource getDataSource();

    protected abstract String getLiquibaseTablePrefix();

    protected abstract String getMigrationScriptSubFolderName();

    private enum Phase {
        PRE_UPDATE("preUpdate.sql"),
        POST_UPDATE("postUpdate.sql");

        private final String externalFile;

        Phase(String externalFile) {
            this.externalFile = externalFile;
        }

        public String getExternalFile() {
            return externalFile;
        }
    }
}
