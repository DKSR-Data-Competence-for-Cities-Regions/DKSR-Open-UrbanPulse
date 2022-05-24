package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.VirtualSensorTO;
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
public class VirtualSensorsWrapperTO {

    public List<VirtualSensorTO> virtualSensors;

    public VirtualSensorsWrapperTO() {
        virtualSensors = new LinkedList<>();
    }

    /**
     * @param virtualSensors  a list of VirtualSensors
     * @throws IllegalArgumentException null list
     */
    public VirtualSensorsWrapperTO(List<VirtualSensorTO> virtualSensors) {
        if (virtualSensors == null) {
            throw new IllegalArgumentException("virtualSensors must not be null");
        }

        this.virtualSensors = virtualSensors;
    }

    public JsonObject toJson() {
        JsonArrayBuilder ab = Json.createArrayBuilder();
        for (VirtualSensorTO to : virtualSensors) {
            ab.add(to.toJson());
        }
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("virtualsensors", ab);
        return ob.build();
    }
}
