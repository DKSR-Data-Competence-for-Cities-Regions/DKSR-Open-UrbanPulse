package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class SensorInformationMapper extends AbstractSensorInformationMapper {

    @Override
    void initInfoMap(JsonObject mapping) {
        infoMapping = new HashMap<>();

        for (String fieldName : mapping.fieldNames()) {
            infoMapping.put(fieldName, mapping.getString(fieldName));
        }
    }

    @Override
    Map<String, String> getAllowedSensorInformation(List<String> listOfSensors) {
        Map<String, String> allowedSensors = new HashMap<>();
        for (String key : infoMapping.keySet()) {
            if (listOfSensors.contains(key)) {
                allowedSensors.put(key, infoMapping.get(key));
            }
        }
        return allowedSensors;
    }

}
