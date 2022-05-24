package de.urbanpulse.urbanpulsecontroller.modules.vertx.jndi;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ClusterConfig {

    private int eventBusPort;
    private String clusterConfigPath;
    private boolean logEventBusContent;
    private int clusterStartupTimeout;

    protected ClusterConfig() {
    }


    ClusterConfig(int eventBusPort, String configPath, boolean logEventBusContent) {
        this.eventBusPort = eventBusPort;
        this.clusterConfigPath = configPath;
        this.logEventBusContent = logEventBusContent;

    }

    protected ClusterConfig setEventBusPort(int eventBusPort) {
        this.eventBusPort = eventBusPort;
        return this;
    }

    protected ClusterConfig setClusterConfigPath(String clusterConfigPath) {
        this.clusterConfigPath = clusterConfigPath;
        return this;
    }

    protected ClusterConfig setLogEventBusContent(boolean logEventBusContent) {
        this.logEventBusContent = logEventBusContent;
        return this;
    }

    protected ClusterConfig setClusterStartupTimeout(int clusterStartupTimeout) {
        this.clusterStartupTimeout = clusterStartupTimeout;
        return this;
    }

    public String getClusterConfigPath() {
        return clusterConfigPath;
    }

    public int getEventBusPort() {
        return eventBusPort;
    }

    public boolean isLogEventBusContent() {
        return logEventBusContent;
    }

    public int getClusterStartupTimeout() {
        return clusterStartupTimeout;
    }

}
