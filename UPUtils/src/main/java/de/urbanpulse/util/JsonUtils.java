package de.urbanpulse.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * A class with utility-/convenience-functions for working with JSON.
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class JsonUtils {

    /**
     * The given Object will be casted to JsonObject if possible. Otherwise an appropriate constructor will be used to create a
     * JsonObject. If no JsonObject can be created from the given Object a ClassCastException will be thrown.
     *
     * @param eventObject the input event to be casted to JsonObject
     * @return the eventObject as JsonObject
     * @throws ClassCastException could not cast to JsonObject
     */
    public static JsonObject toJsonObject(Object eventObject) throws ClassCastException {
        JsonObject event = toJsonObjectOrNull(eventObject);
        if (event == null) {
            throw new ClassCastException("can not cast object of class " + eventObject.getClass().getName() + " to " + JsonObject.class.getName());
        } else {
            return event;
        }
    }

    /**
     * Same as {@link JsonUtils#toJsonObject(java.lang.Object)} except that this function will return null instead of throwing an Exception.
     *
     * @param eventObject the input event to be casted to JsonObject
     * @return the eventObject as JsonObject
     */
    public static JsonObject toJsonObjectOrNull(Object eventObject) {
        JsonObject event = null;
        if (eventObject instanceof JsonObject) {
            // expecting this most often, therefore checking here and not just as default-case
            event = (JsonObject) eventObject;
        } else if (eventObject instanceof Map) {
            // also expected often if more than one PersistenceV3-instances configured
            event = new JsonObject((Map<String, Object>) eventObject);
        } else if (eventObject instanceof String) {
            event = new JsonObject((String) eventObject);
        } else if (eventObject instanceof Buffer) {
            event = new JsonObject((Buffer) eventObject);
        }
        return event;
    }
}
