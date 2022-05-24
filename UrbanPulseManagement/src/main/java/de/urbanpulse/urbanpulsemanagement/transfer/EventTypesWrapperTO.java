package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.EventTypeTO;
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
public class EventTypesWrapperTO {

    public List<EventTypeTO> eventTypes;

    public EventTypesWrapperTO() {
        eventTypes = new LinkedList<>();
    }

    /**
     * @param eventTypes a list of eventTypes
     * @throws IllegalArgumentException null list
     */
    public EventTypesWrapperTO(List<EventTypeTO> eventTypes) {
        if (eventTypes == null) {
            throw new IllegalArgumentException("eventTypes must not be null");
        }

        this.eventTypes = eventTypes;
    }

    public JsonObject toJson() {
        JsonArrayBuilder ab = Json.createArrayBuilder();
        for (EventTypeTO to : eventTypes) {
            ab.add(to.toJson());
        }
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("eventtypes", ab);
        return ob.build();
    }

}
