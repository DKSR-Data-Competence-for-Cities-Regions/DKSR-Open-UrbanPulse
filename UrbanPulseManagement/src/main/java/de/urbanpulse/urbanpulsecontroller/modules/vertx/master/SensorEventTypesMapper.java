package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.dist.jee.entities.EventTypeEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * creates a Json mapping of a sensor's SID to the eventType name it uses (for setup of an {@link UPModuleType#EventProcessor}
 * module)
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class SensorEventTypesMapper {

    @Inject
    private SensorManagementDAO sensorDAO;

    /**
     * @return JsonObject mapping SID to array of eventType names
     */
    public JsonObject readSensorEventTypes() {
        JsonObject sensorEventType = new JsonObject();
        List<SensorEntity> sensors = sensorDAO.queryAllWithDepsFetched();
        for (SensorEntity sensor : sensors) {
            String sid = sensor.getId();
            sensorEventType.put(sid, getEventType(sensor));
        }

        return sensorEventType;
    }

    /**
     * @param sensor the sensor for which  you want to get the event type
     * @return name of the EventType
     */
    private String getEventType(SensorEntity sensor) {
        final EventTypeEntity eventType = sensor.getEventType();
        return eventType.getName();
    }
}
