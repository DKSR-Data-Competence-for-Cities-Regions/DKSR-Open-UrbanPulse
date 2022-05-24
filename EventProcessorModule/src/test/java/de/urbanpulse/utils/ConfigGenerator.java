package de.urbanpulse.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConfigGenerator {

    public static Integer index = 1;

    public static Map<String, Object> joinConfigs(Map<String, Object> c1, Map<String, Object> c2) {
        Map<String, Object> joinedConfig = new HashMap<>();
        joinedConfig.putAll(c1);
        c2.forEach((key, value) -> {
            if (joinedConfig.containsKey(key)) {
                ((List<Map<String, Object>>) joinedConfig.get(key)).addAll((List<Map<String, Object>>) (value));

            } else {
                joinedConfig.put(key, (List<Map<String, Object>>) value);
            }
        });
        return joinedConfig;
    }

    public static Map<String, Object> getSetupConfig(int listenerCount, String eventTypeName) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> eventTypeFields = new HashMap<>();
        Map<String, Object> eventTypeConfig = new HashMap<>();
        Map<String, Object> statementFields = new HashMap<>();
        List<Map<String, Object>> eventTypes = new ArrayList<>();
        List<Map<String, Object>> statements = new ArrayList<>();
        List<Map<String, Object>> listeners = new ArrayList<>();
        String statementName = eventTypeName + "Statement";

        eventTypeFields.put("name", (String) eventTypeName);
        eventTypeConfig.put("property", "java.lang.String");
        eventTypeFields.put("config", eventTypeConfig);
        eventTypes.add(eventTypeFields);

        statementFields.put("name", (String) statementName);
        statementFields.put("query", (String) "SELECT * FROM " + eventTypeName);
        statements.add(statementFields);

        for (int i = 0; i < listenerCount; i++) {
            listeners.add(ConfigGenerator.getUpdateListenerConfig(statementName, "", ConfigGenerator.index++));
        }
        config.put("eventTypes", eventTypes);
        config.put("statements", statements);
        config.put("listeners", listeners);
        return config;
    }

    public static Map<String, Object> getUpdateListenerConfig(String statementName, String vertxAddress, Integer id) {
        Map<String, Object> ulConfig = new HashMap<>();
        if (id != null) {
            ulConfig.put("id", String.valueOf(id));
        }
        if (statementName != null) {
            ulConfig.put("statementName", statementName);
        }
        ulConfig.put("vertxAddress", vertxAddress);
        return ulConfig;
    }
}
