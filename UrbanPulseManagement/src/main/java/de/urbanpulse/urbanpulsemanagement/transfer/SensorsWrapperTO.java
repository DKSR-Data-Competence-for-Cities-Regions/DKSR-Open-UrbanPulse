package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SensorsWrapperTO {

    public List<SensorTO> sensors;

    public SensorsWrapperTO() {
        sensors = new LinkedList<>();
    }

    /**
     * @param sensors a list of sensors
     * @throws IllegalArgumentException null list
     */
    public SensorsWrapperTO(List<SensorTO> sensors) {
        if (sensors == null) {
            throw new IllegalArgumentException("sensors must not be null");
        }

        this.sensors = sensors;
    }

    public JsonObject toJson() {
        List<JsonObject> sensorJsons = sensors.parallelStream()
                        .map(SensorTO::toJson)
                        .collect(Collectors.toList());

        JsonObject job = new JsonObject();
        job.put("sensors", new JsonArray(sensorJsons));
        return job;
    }

}
