package de.urbanpulse.urbanpulsecontroller.modules.vertx;

/**
 * supported types of remote vert.x modules
 * ({@link #OutboundInterface}, {@link #EventProcessor}, {@link #InboundInterface},  {@link #PersistenceV3})
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@SuppressWarnings("java:S115") // We're using these names as strings later down the chain
public enum UPModuleType {

    /**
     * well known node, keeps the cluster alive and aids in orderly startup of the other modules
     */
    WellKnownNode,
    /**
     * sends events received from the event processor to the respective update listener destinations (e.g. via https, websocket,
     * ...), also now includes a REST interface to query persisted historic events
     */
    OutboundInterface,
    /**
     * processes events received from the inbound interface via the Esper engine and sends outgoing events to the outbound
     * interfaces
     */
    EventProcessor,
    /**
     * sends events received from e.g. the Azure EventHub to the event processor
     */
    InboundInterface,
    /**
     * new persistence module
     */
    PersistenceV3,
    /**
     * Backchannel module
     */
    Backchannel
}
