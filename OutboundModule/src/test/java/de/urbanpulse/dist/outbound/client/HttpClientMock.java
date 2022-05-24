package de.urbanpulse.dist.outbound.client;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import java.util.List;
import java.util.function.Function;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpClientMock implements HttpClient {

    private HttpClientRequest request = new HttpClientRequestMock();
    private Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler;

    public HttpClientRequest getRequest() {
        return request;
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod hm, String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod hm, String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest getAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest getAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient getNow(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient getNow(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient getNow(String string, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest post(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest postAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest postAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest headAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest headAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient headNow(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient headNow(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient headNow(String string, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest options(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest optionsAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest optionsAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient optionsNow(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient optionsNow(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClient optionsNow(String string, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest put(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest putAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest putAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest deleteAbs(String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest deleteAbs(String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, WebsocketVersion wv, String string2, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(int i, String string, String string1, MultiMap mm, WebsocketVersion wv, String string2, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, WebsocketVersion wv, String string2, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, String string1, MultiMap mm, WebsocketVersion wv, String string2, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, WebsocketVersion wv, String string1, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(String string, MultiMap mm, WebsocketVersion wv, String string1, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int i, String string, String string1) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, String string1) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int i, String string, String string1, MultiMap mm) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, String string1, MultiMap mm) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int i, String string, String string1, MultiMap mm, WebsocketVersion wv) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, String string1, MultiMap mm, WebsocketVersion wv) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(int i, String string, String string1, MultiMap mm, WebsocketVersion wv, String string2) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, String string1, MultiMap mm, WebsocketVersion wv, String string2) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, MultiMap mm) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, MultiMap mm, WebsocketVersion wv) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(String string, MultiMap mm, WebsocketVersion wv, String string1) {
        return new WebSocketStreamMock();
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isMetricsEnabled() {
        return false;
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest get(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient getNow(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest post(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest post(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest head(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient headNow(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest options(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest options(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient optionsNow(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest put(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest put(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest delete(RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClient websocket(RequestOptions ro, Handler<WebSocket> hndlr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HttpClient websocket(RequestOptions ro, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, WebsocketVersion wv, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, WebsocketVersion wv, String string, Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public HttpClient websocket(RequestOptions ro, MultiMap mm, WebsocketVersion wv, String string, Handler<WebSocket> hndlr, Handler<Throwable> hndlr1) {
        return this;
    }

    @Override
    public HttpClient websocketAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
        return this;
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions ro) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions ro, MultiMap mm) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions ro, MultiMap mm, WebsocketVersion wv) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStream(RequestOptions ro, MultiMap mm, WebsocketVersion wv, String string) {
        return new WebSocketStreamMock();
    }

    @Override
    public ReadStream<WebSocket> websocketStreamAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols) {
        return new WebSocketStreamMock();
    }

    @Override
    public HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> fnctn) {
        this.redirectHandler = fnctn;
        return this;
    }

    @Override
    public Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler() {
        return this.redirectHandler;
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, SocketAddress sa, RequestOptions ro) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, SocketAddress sa, int i, String string, String string1) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, SocketAddress sa, RequestOptions ro, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest request(HttpMethod hm, SocketAddress sa, int i, String string, String string1, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod hm, SocketAddress sa, String string) {
        return getRequest();
    }

    @Override
    public HttpClientRequest requestAbs(HttpMethod hm, SocketAddress sa, String string, Handler<HttpClientResponse> hndlr) {
        return getRequest();
    }

    @Override
    public void webSocket(int i, String string, String string1, Handler<AsyncResult<WebSocket>> hndlr) {
    }

    @Override
    public void webSocket(String string, String string1, Handler<AsyncResult<WebSocket>> hndlr) {
    }

    @Override
    public void webSocket(String string, Handler<AsyncResult<WebSocket>> hndlr) {
    }

    @Override
    public void webSocket(WebSocketConnectOptions wsco, Handler<AsyncResult<WebSocket>> hndlr) {
    }

    @Override
    public void webSocketAbs(String string, MultiMap mm, WebsocketVersion wv, List<String> list, Handler<AsyncResult<WebSocket>> hndlr) {
    }

    @Override
    public HttpClient connectionHandler(Handler<HttpConnection> hndlr) {
        return this;
    }

}
