package de.urbanpulse.dist.outbound.server.auth;

import io.vertx.core.json.JsonObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SecurityManagerInitializerTest {

    @Test(expected = NullPointerException.class)
    public void test_init_emptySecurtiyConfig() {
        SecurityManagerInitializer.initSecurityManager(new JsonObject());
    }

    @Test
    public void test_init_emptyKeycloakConfig() {
        String jdbcConfig = "{\n" +
                "        \"driverClassName\": \"org.postgresql.Driver\",\n" +
                "        \"username\": \"postgres123\",\n" +
                "        \"password\": \"postgres123\",\n" +
                "        \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n" +
                "        \"maxActive\": 8,\n" +
                "        \"maxIdle\": 8,\n" +
                "        \"initialSize\": 1\n" +
                "      }";

        SecurityManagerInitializer.initSecurityManager(new JsonObject().put("jdbc", new JsonObject(jdbcConfig)));
        DefaultSecurityManager securityManager = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
        assertEquals(1, securityManager.getRealms().size());
    }

    @Test
    public void test_init_allConfigs() {
        String jdbcConfig = "{\n" +
                "        \"driverClassName\": \"org.postgresql.Driver\",\n" +
                "        \"username\": \"postgres123\",\n" +
                "        \"password\": \"postgres123\",\n" +
                "        \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n" +
                "        \"maxActive\": 8,\n" +
                "        \"maxIdle\": 8,\n" +
                "        \"initialSize\": 1\n" +
                "      }";

        String keycloakConfig = "{\n" +
                "        \"clientId\": \"test-client\",\n" +
                "        \"secret\": \"9194507c-06f2-423c-8a4d-f7799a481071\",\n" +
                "        \"apiBaseUrl\": \"http://localhost:9080/auth\",\n" +
                "        \"realm\": \"master\"\n" +
                "      }";

        SecurityManagerInitializer.initSecurityManager(new JsonObject()
                .put("jdbc", new JsonObject(jdbcConfig))
                .put("keycloak", new JsonObject(keycloakConfig)));
        DefaultSecurityManager securityManager = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
        assertEquals(2, securityManager.getRealms().size());
    }

    @Test
    public void test_init_empty_keycloak_config() {
        String jdbcConfig = "{\n" +
                "        \"driverClassName\": \"org.postgresql.Driver\",\n" +
                "        \"username\": \"postgres123\",\n" +
                "        \"password\": \"postgres123\",\n" +
                "        \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n" +
                "        \"maxActive\": 8,\n" +
                "        \"maxIdle\": 8,\n" +
                "        \"initialSize\": 1\n" +
                "      }";

        String keycloakConfig = "{\n" +
                "        \"clientId\": \"\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"apiBaseUrl\": \"\",\n" +
                "        \"realm\": \"\"\n" +
                "      }";

        SecurityManagerInitializer.initSecurityManager(new JsonObject()
                .put("jdbc", new JsonObject(jdbcConfig))
                .put("keycloak", new JsonObject(keycloakConfig)));
        DefaultSecurityManager securityManager = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
        assertEquals(2, securityManager.getRealms().size());
    }

    @Test
    public void test_init_noValues_keycloak_config() {
        String jdbcConfig = "{\n" +
                "        \"driverClassName\": \"org.postgresql.Driver\",\n" +
                "        \"username\": \"postgres123\",\n" +
                "        \"password\": \"postgres123\",\n" +
                "        \"url\": \"jdbc:postgresql://localhost:5432/urbanpulse\",\n" +
                "        \"maxActive\": 8,\n" +
                "        \"maxIdle\": 8,\n" +
                "        \"initialSize\": 1\n" +
                "      }";

        String keycloakConfig = "{\n" +
                "      }";

        SecurityManagerInitializer.initSecurityManager(new JsonObject()
                .put("jdbc", new JsonObject(jdbcConfig))
                .put("keycloak", new JsonObject(keycloakConfig)));
        DefaultSecurityManager securityManager = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
        assertEquals(2, securityManager.getRealms().size());
    }

}
