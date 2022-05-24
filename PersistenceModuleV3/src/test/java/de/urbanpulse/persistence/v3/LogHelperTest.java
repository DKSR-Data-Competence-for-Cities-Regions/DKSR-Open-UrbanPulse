package de.urbanpulse.persistence.v3;

import io.vertx.core.json.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class LogHelperTest {

    private static final String YOU_SHOULD_NOT_SEE_THIS = "YOU SHOULD NOT SEE THIS!";

    /**
     * Test of configWithBlankedCredentials method, of class LogHelper.
     */
    @Test
    public void testConfigWithBlankedCredentials() {

        JsonObject config = new JsonObject();
        JsonObject storageConfig = new JsonObject();
        JsonObject secondLevelConfig = new JsonObject();

        JsonObject persistenceMap = new JsonObject();
        persistenceMap.put("password", YOU_SHOULD_NOT_SEE_THIS);
        persistenceMap.put("url", YOU_SHOULD_NOT_SEE_THIS);

        secondLevelConfig.put("connectionstring", YOU_SHOULD_NOT_SEE_THIS);
        secondLevelConfig.put("password", YOU_SHOULD_NOT_SEE_THIS);

        secondLevelConfig.put("persistenceMap", persistenceMap);
        storageConfig.put("secondLevelConfig", secondLevelConfig);
        config.put("storageConfig", storageConfig);

        JsonObject result = LogHelper.configWithBlankedCredentials(config);
        assertFalse(result.encode().contains(YOU_SHOULD_NOT_SEE_THIS));

    }

}
