package de.urbanpulse.urbanpulsemanagement.services.helper;

import java.util.HashSet;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SupportedUPEventParameterTypes {

    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_DATE = "java.util.Date";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_MAP = "java.util.Map";
    public static final String TYPE_LIST = "java.util.List";

    private final Set<String> supportedTypes = new HashSet<>();

    public SupportedUPEventParameterTypes() {
        supportedTypes.add(TYPE_BOOLEAN);
        supportedTypes.add(TYPE_STRING);
        supportedTypes.add(TYPE_DATE);
        supportedTypes.add(TYPE_LONG);
        supportedTypes.add(TYPE_DOUBLE);
        supportedTypes.add(TYPE_MAP);
        supportedTypes.add(TYPE_LIST);
    }

    public boolean contains(String type) {
        return this.supportedTypes.contains(type);
    }

}
