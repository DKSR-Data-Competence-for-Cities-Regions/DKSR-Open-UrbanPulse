package de.urbanpulse.persistence.v3.jpa;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;

/**
 * reads property "de.urbanpulse.eventTableName" from JPA persistenceMap and configures EclipseLink to use its value as table name
 * for {@link JPAEventEntity} instead of the default "up_events" specified in the annotation
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EclipseLinkTableNameSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        String tableName = (String) session.getProperty("de.urbanpulse.eventTableName");
        if (tableName != null) {
            session.getClassDescriptor(JPAEventEntity.class).setTableName(tableName);
        }
    }
}
