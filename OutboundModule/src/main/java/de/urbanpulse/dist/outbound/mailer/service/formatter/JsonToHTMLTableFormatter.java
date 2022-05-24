package de.urbanpulse.dist.outbound.mailer.service.formatter;

import io.vertx.core.json.JsonObject;
import java.util.Map.Entry;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public class JsonToHTMLTableFormatter implements EmailFormatter {

    private JsonObject config;

    @Override
    public String format(JsonObject event) {
        StringBuilder builder = new StringBuilder();
        builder.append(createTitle());
        builder.append("<table style=\"width:100%;border:1px solid black;border-collapse: collapse;\">");
        event.stream().map(this::toHTMLRow)
                .forEach(builder::append);

        builder.append("</table>");
        return builder.toString();
    }

    private String tr(String input) {
        return "<tr>" + input + "</tr>";
    }

    private String td(String input) {
        return "<td style=\"border:1px solid black;border-collapse: collapse;\">" + input + "</td>";
    }

    private String toHTMLRow(Entry<String, Object> entry) {
        String value = entry.getValue()==null?"null":entry.getValue().toString();
        return tr(td(styleRowName(entry.getKey())) + td(value));
    }

    private String styleRowName(String input) {
        return "<b>" + input + "</b>";
    }

    @Override
    public boolean requiresHTML() {
        return true;
    }

    private String createTitle() {
        if (null != config && config.containsKey("title")) {
            return "<h2>" + config.getString("title") + "</h2>";
        } else {
            return "";
        }
    }

    @Override
    public void setConfig(JsonObject config) {
        this.config = config;
    }

}
