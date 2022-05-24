package de.urbanpulse.urbanpulsecontroller.modules.vertx.jndi;

import de.urbanpulse.urbanpulsecontroller.modules.vertx.jndi.options.BlockedThreadCheckInterval;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ConfigLookup {

    private static final String EVENTBUS_PORT_JNDI_NAME = "cluster/eventbusPort";
    private static final String BLOCKED_THREAD_CHECK_INTERVAL_JNDI_NAME = "vertx/BlockedThreadCheckInterval";
    private static final String HAZELCAST_PATH_CLUSTER_CONFIG_JNDI_NAME = "hazelcast/pathClusterConfig";
    private static final String LOG_EVENT_BUS_CONTENT = "urbanpulse/logEventBusContent";
    private static final String CLUSTER_START_TIMEOUT = "urbanpulse/clusterStartTimeout";

    private InitialContext initialContext;
    private static final Logger LOG = Logger.getLogger(ConfigLookup.class.getName());

    @Produces
    public ClusterConfig getClusterConfig() {

        int eventBusPort = (Integer) checkLookupResult(EVENTBUS_PORT_JNDI_NAME, 0);
        LOG.log(Level.INFO, "Event Bus Port: {0}", eventBusPort);
        String configPath = (String) checkLookupResult(HAZELCAST_PATH_CLUSTER_CONFIG_JNDI_NAME, null);
        LOG.log(Level.INFO, "Config File Path: {0}", configPath);
        boolean logEventBusContent = (Boolean) checkLookupResult(LOG_EVENT_BUS_CONTENT, false);
        LOG.log(Level.INFO, "Log Event Bus Content: {0}", logEventBusContent);
        int clusterTimeout = (int) checkLookupResult(CLUSTER_START_TIMEOUT, 60);
        LOG.log(Level.INFO, "Cluster start timeout: {0}", clusterTimeout);

        return new ClusterConfig()
                .setClusterConfigPath(configPath)
                .setEventBusPort(eventBusPort)
                .setLogEventBusContent(logEventBusContent)
                .setClusterStartupTimeout(clusterTimeout);
    }

    @Produces
    @BlockedThreadCheckInterval
    public Integer getBlockedThreadCheckInterval() {
        return (Integer) checkLookupResult(BLOCKED_THREAD_CHECK_INTERVAL_JNDI_NAME, 1000);
    }

    @PostConstruct
    public void init() {
        try {
            initialContext = new InitialContext();
        } catch (NamingException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "unable to get initial context", ex);
        }
    }

    private Object checkLookupResult(String jndiName, Object defaultValue) {
        try {
            return initialContext.lookup(jndiName);
        } catch (NamingException ex) {
            LOG.log(Level.INFO, "Lookup for {0} failed using default {1}", new Object[]{jndiName, defaultValue});
            return defaultValue;
        }
    }
}
