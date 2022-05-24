package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateless;
import javax.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class EventConfigValidator {

    private static final Set<String> VALID_TYPE_NAMES;
    private static final String MANDATORY_FIELD_SID = "SID";
    private static final String MANDATORY_FIELD_TIMESTAMP = "timestamp";

    static {
        VALID_TYPE_NAMES = new HashSet<>();
        VALID_TYPE_NAMES.add(EventParamTypes.DATE);
        VALID_TYPE_NAMES.add(EventParamTypes.LONG);
        VALID_TYPE_NAMES.add(EventParamTypes.STRING);
        VALID_TYPE_NAMES.add(EventParamTypes.DOUBLE);
        VALID_TYPE_NAMES.add(EventParamTypes.BOOLEAN);
        VALID_TYPE_NAMES.add(EventParamTypes.MAP);
        VALID_TYPE_NAMES.add(EventParamTypes.LIST);
    }

    /**
     * @param eventConfig the event to be checked
     * @return true if eventConfig is either empty or contains at least one
     * value which differs from those defined in {@link #VALID_TYPE_NAMES}
     * or doesn't contain the key SID and timestamp
     */
    public boolean isInvalid(JsonObject eventConfig) {

        if (eventConfig.isEmpty()) {
            return true;
        }
        boolean foundTimestamp = false;
        boolean foundSID = false;
        for (String eventTypeParam : eventConfig.keySet()) {
            String typeName = eventConfig.getString(eventTypeParam);
            if (!VALID_TYPE_NAMES.contains(typeName)) {
                return true;
            }
            if (!foundTimestamp && MANDATORY_FIELD_TIMESTAMP.equals(eventTypeParam) && EventParamTypes.DATE.equals(typeName)) {
                foundTimestamp = true;
            }
            if (!foundSID && MANDATORY_FIELD_SID.equals(eventTypeParam) && EventParamTypes.STRING.equals(typeName)) {
                foundSID = true;
            }
        }

        return !foundSID || !foundTimestamp;

    }
}
