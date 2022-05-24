package de.urbanpulse.dist.outbound.mailer.service.formatter;

import io.vertx.core.json.JsonObject;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public class DefaultEmailFormatter implements EmailFormatter {

    @Override
    public String format(JsonObject event) {
        return event.encodePrettily();
    }

    @Override
    public boolean requiresHTML() {
        return false;
    }

    @Override
    public void setConfig(JsonObject config) {
    }

}
