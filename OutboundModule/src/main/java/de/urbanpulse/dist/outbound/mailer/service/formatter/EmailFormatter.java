package de.urbanpulse.dist.outbound.mailer.service.formatter;

import io.vertx.core.json.JsonObject;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public interface EmailFormatter {
    public String format(JsonObject event);
    public void setConfig(JsonObject config);
    public boolean requiresHTML();
}
