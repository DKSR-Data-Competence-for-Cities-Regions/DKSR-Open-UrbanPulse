package de.urbanpulse.dist.inbound.http.auth;

import com.hazelcast.util.Base64;
import de.urbanpulse.dist.inbound.http.HttpInboundCommandHandler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.junit.RunTestOnContext;
import java.io.UnsupportedEncodingException;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class MessageAuthenticatorTest {

    MessageAuthenticator authenticator;

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setUp() throws UnsupportedEncodingException {
        UPAuthHeaderDecoder authHeaderDecoder = new UPAuthHeaderDecoder();
        Vertx vertx = rule.vertx();
        LocalMap<String, String> connectorAuth = vertx.sharedData()
                .getLocalMap(HttpInboundCommandHandler.CONNECTOR_AUTH_MAP_NAME);
        connectorAuth.put(new String("?Z?s"), "mmourao");
        authenticator = new MessageAuthenticator(authHeaderDecoder, connectorAuth);
    }

    @Test()
    public void testisAuthenticated() throws UnsupportedEncodingException {

        byte[] encodedUserName = Base64.encode(new String("?Z?s").getBytes("UTF-8"));
        StringBuilder authHeaderBuilder = new StringBuilder();
        authHeaderBuilder.append("UP ")
                .append(new String(encodedUserName))
                .append(":twL+WuNuBDFeXan7sgebZdMSiLhnFj782tHpY2Zjfb0=");
        String dataToHash = "softwareDeveloper";
        assertTrue(authenticator.isAuthenticated(authHeaderBuilder.toString(), dataToHash));
    }
}
