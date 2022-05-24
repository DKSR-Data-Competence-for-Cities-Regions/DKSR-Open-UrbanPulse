package city.ui.shared.commons.collections;

import java.util.Map;
import java.util.TreeMap;

/**
 * A sorted map whose keys are treated case insensitively
 * @param <V> value type
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CaseInsensitiveSortedMap<V> extends TreeMap<String, V> {

    /**
     * Constructor. call the parent constructor
     */
    public CaseInsensitiveSortedMap() {
        super();
    }

    /**
     * Constructor. calls the parent constructor with the specified map
     *
     * @param map
     */
    public CaseInsensitiveSortedMap(Map<String, ? extends V> map) {
        super(map);
    }

    /**
     * Associates the specified value with the specified key in the map, key is converted to lower case
     *
     * @param key
     * @param value
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    @Override
    public V put(String key, V value) {
        return super.put(key.toLowerCase(), value);
    }

    /**
     * Returns the value to which the specified key is mapped (lower-cased), or null if this map contains no mapping for the key.
     *
     * @param key
     * @return the mapped value
     */
    @Override
    public V get(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Key must be of type String");
        }
        return super.get(((String) key).toLowerCase());
    }
}
