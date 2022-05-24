package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
abstract class AbstractSensorInformationMapper {

    protected Map<String, String> infoMapping;

    abstract void initInfoMap(JsonObject mapping);

    abstract Map<String, String> getAllowedSensorInformation(List<String> listOfSensors);
}
