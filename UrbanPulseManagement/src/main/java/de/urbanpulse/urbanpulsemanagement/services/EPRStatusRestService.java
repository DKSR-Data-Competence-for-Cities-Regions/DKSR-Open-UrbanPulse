package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapperException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class EPRStatusRestService extends AbstractRestService {

    private static final String KEY_EVENTS_PROCESSED = "events_processed";

    @Inject
    EventProcessorWrapper eventProcessor;

    /**
     * get event processor status
     *
     * @param key currently the only supported value is "events_processed"
     * @return 200 OK with status EPR status JSON like '{ "KEY": SOME_VALUE }' if key is supported, 400 BAD
     * REQUEST if key is unsupported
     */
    public Response getEprStatus(@QueryParam("key") String key) {
        if (KEY_EVENTS_PROCESSED.equals(key)) {
            long events;
            try {
                events = eventProcessor.countProcessedEvents();
            } catch (EventProcessorWrapperException ex) {
                return ErrorResponseFactory.internalServerErrorFromException(ex);
            }
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
            jsonBuilder.add(KEY_EVENTS_PROCESSED, events);
            return Response.ok(jsonBuilder.build().toString()).build();
        } else {
            return ErrorResponseFactory.badRequest("parameter 'key' is missing");
        }
    }
}
