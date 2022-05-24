package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.exceptions.VertxClusterException;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import de.urbanpulse.transfer.TransportLayer;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.jndi.ClusterConfig;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.master.transfer.TransportLayerJEE;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileNotFoundException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Singleton
public class TransportLayerFactory {

    private static final Logger LOGGER = Logger.getLogger(TransportLayerFactory.class.getName());

    private Vertx vertxInstance;

    @Inject
    private ClusterConfig clusterConfig;

    @EJB
    private AsyncConnectionHandler asyncer;

    @Produces
    @VertxEmbedded
    public TransportLayer getTransportLayer() {
        if (null == vertxInstance) {
            throw new VertxClusterException("Vert.x cluster not initialized!");
        }
        return new TransportLayerJEE(vertxInstance, asyncer, clusterConfig.isLogEventBusContent());
    }

    @PreDestroy
    private void destroy() {
        if (this.vertxInstance != null) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                vertxInstance.close((AsyncResult<Void> event) -> latch.countDown());

                boolean success = latch.await(10, TimeUnit.SECONDS);
                if (!success) {
                    throw new VertxClusterException("Timeout on while cluster shutdown.");
                }
            } catch (InterruptedException | VertxClusterException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getHostAddress() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            throw new DeploymentException("Vertx cluster not started. Network Interface could not be determined!", ex);
        }
        String hostAddress = inetAddress.getHostAddress();
        LOGGER.log(Level.INFO, "IP Address is: {0}", hostAddress);
        return hostAddress;
    }

    private Config getClusterConfig() {
        String clusterConfigPath = clusterConfig.getClusterConfigPath();
        Config config;

        if (null != clusterConfigPath) {
            LOGGER.log(Level.INFO, "cluster config path: {0}", clusterConfigPath);
            try {
                config = new XmlConfigBuilder(clusterConfigPath).build();
            } catch (FileNotFoundException ex) {
                throw new DeploymentException("Vertx cluster not started. Cluster config not found!", ex);
            }
        } else {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            LOGGER.log(Level.WARNING, "Using default cluster config.");
            config = new XmlConfigBuilder(classloader.getResourceAsStream("default-up-cluster.xml")).build();
        }
        LOGGER.log(Level.INFO, "Cluster config: {0}", config);
        return config;
    }

    @PostConstruct
    private void init() {

        Config config = getClusterConfig();
        ClusterManager clusterManager = new HazelcastClusterManager(config);

        VertxOptions options = new VertxOptions();
        options.setClusterManager(clusterManager);
        options.setEventBusOptions(new EventBusOptions().setPort(clusterConfig.getEventBusPort()).setHost(getHostAddress()));

        CountDownLatch latch = new CountDownLatch(1);
        LOGGER.log(Level.INFO, "Starting Vert.x cluster!");
        long millies = System.currentTimeMillis();

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                vertxInstance = res.result();
                LOGGER.log(Level.INFO, "Vert.x cluster started! Took {0} ms.", System.currentTimeMillis() - millies);
                latch.countDown();
            } else {
                LOGGER.log(Level.SEVERE, "Failed to start vert.x cluster!", res.cause());
            }
        });

        try {
            boolean success = latch.await(clusterConfig.getClusterStartupTimeout(), TimeUnit.SECONDS);
            if (!success) {
                throw new VertxClusterException("Timeout on while cluster startup.");
            }
        } catch (InterruptedException ex) {
            // Restore interrupted state... (SQ says that's a good idea).
            Thread.currentThread().interrupt();
            throw new VertxClusterException("Vert.x cluster could not be started!", ex);
        }
    }
}
