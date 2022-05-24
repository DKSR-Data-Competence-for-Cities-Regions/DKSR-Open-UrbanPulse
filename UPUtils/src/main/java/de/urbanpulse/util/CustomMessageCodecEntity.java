package de.urbanpulse.util;

import io.vertx.core.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface CustomMessageCodecEntity {

    CustomMessageCodecEntity fromJson(JsonObject obj);

    JsonObject toJson();
}
