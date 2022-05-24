package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;
import java.util.LinkedList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConnectorsWrapperTO {

    public List<ConnectorTO> connectors;

    public ConnectorsWrapperTO() {
        connectors = new LinkedList<>();
    }

    /**
     * @param connectors list of connectors
     * @throws IllegalArgumentException null list
     */
    public ConnectorsWrapperTO(List<ConnectorTO> connectors) {
        if (connectors == null) {
            throw new IllegalArgumentException("connectors must not be null");
        }

        this.connectors = connectors;
    }

    public JsonObject toJson() {
        JsonArrayBuilder ab = Json.createArrayBuilder();
        for (ConnectorTO to : connectors) {
            ab.add(to.toJson());
        }
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("connectors", ab);
        return ob.build();
    }

}
