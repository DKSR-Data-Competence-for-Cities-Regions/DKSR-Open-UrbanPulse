package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.ModuleUpdateManager;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * wrapper for sending commands to PersistenceV3 modules
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
public class PersistenceV3Wrapper {

    @Inject
    private CommandsJsonFactory commandsJsonFactory;

    @Inject
    private ModuleUpdateManager moduleUpdateManager;
}
