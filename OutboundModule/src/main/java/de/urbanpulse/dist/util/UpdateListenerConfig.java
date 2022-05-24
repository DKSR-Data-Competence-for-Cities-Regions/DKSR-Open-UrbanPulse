package de.urbanpulse.dist.util;

import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UpdateListenerConfig {

    private String id;
    private String statementName;
    private String target;

    public UpdateListenerConfig(String id, String statementName, String target) {
        this.id = id;
        this.statementName = statementName;
        this.target = target;
    }

    public UpdateListenerConfig(JsonObject json) {
        this.id = json.getString("id");
        this.statementName = json.getString("statementName");
        this.target = json.getString("target");
    }

    public boolean isValid() {
        return id != null && statementName != null && target != null;
    }

    public String getId() {
        return id;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "UpdateListenerConfig{" + "id='" + id + '\'' + ", statementName='" + statementName + '\'' + ", target='" + target
                + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UpdateListenerConfig that = (UpdateListenerConfig) o;
        return Objects.equals(id, that.id) && Objects.equals(statementName, that.statementName) && Objects
                .equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, statementName, target);
    }
}
