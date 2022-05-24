package de.urbanpulse.persistence.v3;

import io.vertx.core.json.JsonObject;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class LogHelper {

    public static JsonObject configWithBlankedCredentials(JsonObject config) {
        JsonObject obfuscated = config.copy();

        JsonObject secondLevelConfig = obfuscated.getJsonObject("storageConfig", new JsonObject())
                .getJsonObject("secondLevelConfig", new JsonObject());

        Set<String> secondLevelKeys = secondLevelConfig.getMap().keySet();
        for (String keyStr : secondLevelKeys) {
            if (keyStr.equalsIgnoreCase("connectionstring") || keyStr.equalsIgnoreCase("password")){
                secondLevelConfig.put(keyStr, "<BLANKED>");
            }
        }

        if (secondLevelConfig.containsKey("persistenceMap")) {
            JsonObject persistenceMap = secondLevelConfig.getJsonObject("persistenceMap");
            for (String fieldName : persistenceMap.fieldNames()) {
                String lowerCaseFieldName = fieldName.toLowerCase();
                if (lowerCaseFieldName.contains("password") || lowerCaseFieldName.contains("url")) {
                    persistenceMap.put(fieldName, "<BLANKED>");
                }
            }
        }

        JsonObject userAuth = obfuscated.getJsonObject("userAuth", new JsonObject());
        for (String userName : userAuth.fieldNames()) {
            JsonObject credentials = userAuth.getJsonObject(userName);
            credentials.put("hmacKey", "<BLANKED>");
            credentials.put("passwordHash", "<BLANKED>");
        }
        return obfuscated;
    }

    private LogHelper() {
        //Not intended to be instantiated.
    }
}
