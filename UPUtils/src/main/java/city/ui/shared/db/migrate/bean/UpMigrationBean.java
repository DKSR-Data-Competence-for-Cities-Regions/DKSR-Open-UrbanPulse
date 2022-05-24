package city.ui.shared.db.migrate.bean;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Alternative
@ApplicationScoped
public class UpMigrationBean extends AbstractMigrationBean {

    private static final Logger LOG = Logger.getLogger(UpMigrationBean.class.getName());

    private static final String LOG_LOCK_TABLE_PREFIX = "lb_up_";
    private static final String UP_MIGRATION_SCRIPT_SUB_FOLDER_NAME = "up";

    @Resource(lookup = "UPManagement")
    private DataSource dataSource;

    @Override
    protected String getLiquibaseTablePrefix() {
        LOG.info("Returning Liquibase table prefix of UP Management");
        return LOG_LOCK_TABLE_PREFIX;
    }

    @Override
    protected String getMigrationScriptSubFolderName() {
        return UP_MIGRATION_SCRIPT_SUB_FOLDER_NAME;
    }

    @Override
    protected DataSource getDataSource() {
        LOG.info("Returning data source of UP Management");
        return dataSource;
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("Initializing UpMigrationBean");
        doMigrate();
    }
}
