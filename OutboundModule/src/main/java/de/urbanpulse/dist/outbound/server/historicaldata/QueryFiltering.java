package de.urbanpulse.dist.outbound.server.historicaldata;

import city.ui.shared.commons.time.UPDateTimeFormat;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
class QueryFiltering {

    private Long defaultMaxAgeInDays = null;
    private JsonArray defaultEventParameterExcludeFilter = null;

    private Map<String, Long> sidMaxAgeInDays = new HashMap<>();
    private JsonArray sidEventParameterExcludeFilter = null;

    private Long defaultMaxIntervalSizeInMinutes = null;
    private Map<String, Long> sidMaxIntervalSizeInMinutes = new HashMap<>();

    private final Logger logger;

    private final List<String> sids;

    public QueryFiltering(JsonObject config, List<String> sids) {
        logger = LoggerFactory.getLogger(QueryFiltering.class.getName());
        this.sids = sids;

        JsonObject defaultFilters = config.getJsonObject("defaultFilters", new JsonObject());
        JsonObject defaultRequestFilters =
                defaultFilters.getJsonObject("requestFilters", new JsonObject());
        JsonObject defaultEventFilters =
                defaultFilters.getJsonObject("eventFilters", new JsonObject());
        defaultMaxAgeInDays = defaultRequestFilters.getLong("maxAgeInDays", 0L);
        defaultEventParameterExcludeFilter =
                defaultEventFilters.getJsonArray("eventParameterExcludeFilter", new JsonArray());
        defaultMaxIntervalSizeInMinutes =
                defaultRequestFilters.getLong("maxIntervalSizeInMinutes", Long.MAX_VALUE);
        if (defaultMaxIntervalSizeInMinutes <= 0) {
            defaultMaxIntervalSizeInMinutes = Long.MAX_VALUE;
        }

        sids.stream().forEach(sid -> {
            JsonObject sidFilters = config.getJsonObject("sidFilters", new JsonObject())
                    .getJsonObject(sid, new JsonObject());
            if (sidFilters.size() > 0) {
                JsonObject sidRequestFilters =
                        sidFilters.getJsonObject("requestFilters", defaultRequestFilters);
                JsonObject sidEventFilters =
                        sidFilters.getJsonObject("eventFilters", defaultEventFilters);
                sidMaxAgeInDays.put(sid, sidRequestFilters.getLong("maxAgeInDays", defaultMaxAgeInDays));

                if (defaultEventParameterExcludeFilter.size() == 0) {
                    sidEventParameterExcludeFilter = sidEventFilters
                            .getJsonArray("eventParameterExcludeFilter", new JsonArray());
                } else {
                    for (int i = 0; i < defaultEventParameterExcludeFilter.size(); ++i) {
                        sidEventParameterExcludeFilter = sidEventFilters
                                .getJsonArray("eventParameterExcludeFilter", new JsonArray())
                                .add(defaultEventParameterExcludeFilter.getString(i));
                    }
                }
                sidMaxIntervalSizeInMinutes.put(sid, sidRequestFilters.getLong("maxIntervalSizeInMinutes",
                        defaultMaxIntervalSizeInMinutes));
            } else {
                sidMaxAgeInDays.put(sid, defaultMaxAgeInDays);
                sidEventParameterExcludeFilter = defaultEventParameterExcludeFilter;
                sidMaxIntervalSizeInMinutes.put(sid, defaultMaxIntervalSizeInMinutes);
            }
        });

    }

    public List<String> filter(String since, String until) {
        return sids.stream().filter(sid -> isQueryValid(sid, since, until))
          .collect(Collectors.toList());
    }

    public boolean isQueryValid(String sid, String since, String until) {
        if (since == null && until == null) {
            // last event
            return true;
        } else if (since == null || until == null) {
            // else both must be provided
            return false;
        }

        try {
            Long sinceMillis = ZonedDateTime.parse(since, UPDateTimeFormat.getFormatterWithZoneZ())
                    .toInstant().toEpochMilli();
            Long untilMillis = ZonedDateTime.parse(until, UPDateTimeFormat.getFormatterWithZoneZ())
                    .toInstant().toEpochMilli();
            Long sinceUntilDifferenceInMs = untilMillis - sinceMillis;
            Long sidMaxIntervalSizeInMs = TimeUnit.MINUTES.toMillis(sidMaxIntervalSizeInMinutes.get(sid));
            ZonedDateTime dt = ZonedDateTime.now();
            ZonedDateTime dateAllowed = dt.minusDays(sidMaxAgeInDays.get(sid).intValue());
            ZonedDateTime sinceDate =
                    ZonedDateTime.parse(since, UPDateTimeFormat.getFormatterWithZoneZ());

            if ((sidMaxAgeInDays.get(sid) > 0 && sinceDate.isBefore(dateAllowed))
                    || sinceUntilDifferenceInMs < 0) {
                return false;
            } else if (sidMaxAgeInDays.get(sid) <= 0 && sinceUntilDifferenceInMs <= sidMaxIntervalSizeInMs) {
                return true;
            } else if (sidMaxAgeInDays.get(sid) > 0 && (sinceUntilDifferenceInMs <= sidMaxIntervalSizeInMs
                    || sidMaxIntervalSizeInMs <= 0)) {
                return true;
            }
            return sinceUntilDifferenceInMs <= sidMaxIntervalSizeInMs;
        } catch (RuntimeException e) {
            logger.error("error parsing date. date in parameter not conform with format.");
            return false;
        }
    }

    /**
     * removes fields from the event, based on any exclude filters present in the config.json and
     * the includeOnly set from the optional query parameter
     *
     * @param event       the JsonObject representing the event
     * @param includeOnly if non-null, then also remove any fields not present there in addition to
     *                    those excluded via config.json
     * @return a reference to msgBody (after removal of filtered fields)
     */
    public JsonObject applyEventFilter(JsonObject event, Set<String> includeOnly) {

        if (sidEventParameterExcludeFilter != null) {
            for (int i = 0; i < sidEventParameterExcludeFilter.size(); ++i) {
                String filteredField = sidEventParameterExcludeFilter.getString(i);
                event.remove(filteredField);
            }
        }

        if (includeOnly != null) {
            Set<String> fieldNames = new HashSet<>(event.fieldNames());
            for (String fieldName : fieldNames) {
                if (includeOnly.contains(fieldName)) {
                    continue;
                }

                event.remove(fieldName);
            }
        }

        return event;
    }
}
