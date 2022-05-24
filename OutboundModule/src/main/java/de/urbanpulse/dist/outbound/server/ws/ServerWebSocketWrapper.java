package de.urbanpulse.dist.outbound.server.ws;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.auth.User;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ServerWebSocketWrapper {

    private final ServerWebSocket websocket;
    private final User user;

    public ServerWebSocketWrapper(ServerWebSocket websocket, User user) {
        this.user = user;
        this.websocket = websocket;
    }

    public ServerWebSocket getWebsocket() {
        return websocket;
    }

    public User getUser() {
        return user;
    }

}
