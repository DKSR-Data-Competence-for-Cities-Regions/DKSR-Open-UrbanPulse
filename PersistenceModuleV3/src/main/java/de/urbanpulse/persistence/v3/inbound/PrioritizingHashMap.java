package de.urbanpulse.persistence.v3.inbound;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This is an extended HashMap of LinkedBlockingQueues. It offers the special function drainMostImportant
 * that drains the most important queue to the given collection. Details in description of that method
 *
 * @author <a href="stephan.spielmann@the-urban-institute.de">Stephan Spielmann</a>
 */
public class PrioritizingHashMap extends HashMap<String, LinkedBlockingQueue<JsonObject>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizingHashMap.class);

    private final HashMap<String, Instant> lastDrainingTimes = new HashMap<>();
    private int maxElements = 100;
    private long maxTimeMillis = 5000;

    public PrioritizingHashMap(int maxElements, long maxTimeMillis) {
        this.maxElements = maxElements;
        this.maxTimeMillis = maxTimeMillis;
    }

    public long getMaxTimeMillis() {
        return maxTimeMillis;
    }

    public void setMaxTimeMillis(long maxTimeMillis) {
        this.maxTimeMillis = maxTimeMillis;
    }

    public int getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(int maxElements) {
        this.maxElements = maxElements;
    }

    @Override
    public LinkedBlockingQueue<JsonObject> put(String key, LinkedBlockingQueue<JsonObject> value) {
        LinkedBlockingQueue<JsonObject> returnValue = super.put(key, value);
        lastDrainingTimes.putIfAbsent(key, Instant.now());
        return returnValue;
    }

    @Override
    public LinkedBlockingQueue<JsonObject> putIfAbsent(String key, LinkedBlockingQueue<JsonObject> value) {
        LinkedBlockingQueue<JsonObject> returnValue = super.putIfAbsent(key, value);
        lastDrainingTimes.putIfAbsent(key, Instant.now());
        return returnValue;
    }

    @Override
    public void clear() {
        super.clear();
        lastDrainingTimes.clear();
    }

    @Override
    public LinkedBlockingQueue<JsonObject> remove(Object key) {
        LinkedBlockingQueue<JsonObject> returnValue = super.remove(key);
        lastDrainingTimes.remove(key);
        return returnValue;
    }

    /**
     * Drains the Queue that is most important at the moment.
     * Priorities are as following:
     * -Queues that haven't been processed for more than 5 seconds
     * -Queues with more than 100 elements
     * -Queues that were not drained the longest time
     *
     * @param c a collection where the most important queue gets drained with {@link LinkedBlockingQueue#drainTo(java.util.Collection)}
     * @return The SID of the queue that was drained or an empty optional if nothing was drained
     */
    public Optional<String> drainMostImportant(List<JsonObject> c) {
        long start = System.currentTimeMillis();
        Optional<Map.Entry<String, LinkedBlockingQueue<JsonObject>>> drainee = determineNextDrainee();
        Optional<String> drainedSid;
        if (!drainee.isPresent()) {
            drainedSid = Optional.empty();
        } else {
            drainee.get().getValue().drainTo(c, maxElements);
            lastDrainingTimes.put(drainee.get().getKey(), Instant.now());
            drainedSid = Optional.of(drainee.get().getKey());
            long end = System.currentTimeMillis();
            LOGGER.debug("Drained {0}, took {1} ms", drainedSid, end - start);
        }

        return drainedSid;
    }

    Optional<Map.Entry<String, LinkedBlockingQueue<JsonObject>>> determineNextDrainee() {
        final Instant now = Instant.now();

        return this.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .min(
                        (Map.Entry<String, LinkedBlockingQueue<JsonObject>> o1,
                                Map.Entry<String, LinkedBlockingQueue<JsonObject>> o2) -> {
                            String sid1 = o1.getKey();
                            String sid2 = o2.getKey();

                            Instant lastDrainingTime1 = lastDrainingTimes.get(sid1);
                            Instant lastDrainingTime2 = lastDrainingTimes.get(sid2);

                            Long age1 = now.toEpochMilli() - lastDrainingTime1.toEpochMilli();
                            Long age2 = now.toEpochMilli() - lastDrainingTime2.toEpochMilli();

                            boolean olderThanMaxTimeMillis1 = age1 > maxTimeMillis;
                            boolean olderThanMaxTimeMillis2 = age2 > maxTimeMillis;

                            // First criterion: Any older than maxTimeMillis?
                            if (olderThanMaxTimeMillis1 && olderThanMaxTimeMillis2) {
                                // Both older than maxTimeMillis - oldest draining age first
                                return age2.compareTo(age1);
                            } else if (olderThanMaxTimeMillis1) {
                                return -1;
                            } else if (olderThanMaxTimeMillis2) {
                                return 1;
                            }

                            // Second criterion: Any more than maxElements elements?
                            boolean moreThanMaxElements1 = o1.getValue().size() > maxElements;
                            boolean moreThanMaxElements2 = o2.getValue().size() > maxElements;
                            if (moreThanMaxElements1 && moreThanMaxElements2) {
                                // Both have more than maxElements - compare by size, then by age
                                if (o1.getValue().size() != o2.getValue().size()) {
                                    return o2.getValue().size() - o1.getValue().size();
                                } else {
                                    return age2.compareTo(age1);
                                }
                            } else if (moreThanMaxElements1) {
                                return -1;
                            } else if (moreThanMaxElements2) {
                                return 1;
                            }

                            // Third criterion: The boring one
                            return age2.compareTo(age1);
                        });
    }

    /**
     *
     * @return number of events in all queues
     */
    public int getTotalCount() {
        return keySet().stream().mapToInt(key -> get(key).size()).sum();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + this.maxElements;
        hash = 89 * hash + (int) (this.maxTimeMillis ^ (this.maxTimeMillis >>> 32));
        hash = 89 * hash + super.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final PrioritizingHashMap other = (PrioritizingHashMap) obj;
        if (this.maxElements != other.maxElements) {
            return false;
        }
        
        return this.maxTimeMillis == other.maxTimeMillis;
    }
}
