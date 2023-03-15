package de.urbanpulse.urbanpulsemanagement.services.helper;

import java.util.HashSet;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SupportedJsonSchemaTypes {

    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_STRING = "string";

    private final Set<String> supportedTypes = new HashSet<>();

    public SupportedJsonSchemaTypes() {
        supportedTypes.add(TYPE_NUMBER);
        supportedTypes.add(TYPE_INTEGER);
        supportedTypes.add(TYPE_ARRAY);
        supportedTypes.add(TYPE_OBJECT);
        supportedTypes.add(TYPE_STRING);
    }

    public boolean contains(String type) {
        return this.supportedTypes.contains(type);
    }
}
