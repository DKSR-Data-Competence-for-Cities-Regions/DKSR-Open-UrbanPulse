package de.urbanpulse.eventbus.helpers;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public enum EventBusImplementationDefaultsHolder {
    DEFAULT_EVENTBUS_FACTORY("de.urbanpulse.eventbus.vertx.VertxEventbusFactory");

    private final String text;

    EventBusImplementationDefaultsHolder(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
