package de.urbanpulse.urbanpulsemanagement;

import city.ui.shared.db.migrate.bean.MigrationBean;
import de.urbanpulse.urbanpulsecontroller.config.DatabaseInitializer;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Startup
@Singleton
public class LiquibaseInitializer {

    private final static Logger LOG = Logger.getLogger(LiquibaseInitializer.class.getName());

    @Inject
    private MigrationBean migrationBean;

    @Inject
    private DatabaseInitializer databaseInitializer;

    @PostConstruct
    public void init() {
        LOG.info("Initializing bean");
        migrationBean.doMigrate();
        databaseInitializer.initializeDb();
    }

}
