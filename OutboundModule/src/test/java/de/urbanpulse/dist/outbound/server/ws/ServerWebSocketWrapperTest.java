package de.urbanpulse.dist.outbound.server.ws;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.auth.User;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerWebSocketWrapperTest {

    private final ServerWebSocket webSocket = Mockito.mock(ServerWebSocket.class);
    private final User user = Mockito.mock(User.class);

    private ServerWebSocketWrapper socketWrapper;

    @Before
    public void setUp() {
        socketWrapper = new ServerWebSocketWrapper(webSocket, user);
    }

    /**
     * Test of getWebsocket method, of class ServerWebSocketWrapper.
     */
    @Test
    public void testGetWebsocket() {
        assertEquals(webSocket, socketWrapper.getWebsocket());
    }

    /**
     * Test of getUser method, of class ServerWebSocketWrapper.
     */
    @Test
    public void testGetUser() {
       assertEquals(user, socketWrapper.getUser());
    }

}
