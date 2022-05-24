package de.urbanpulse.dist.outbound.client;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.WebSocket;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WebSocketStreamMock implements ReadStream<WebSocket> {

    @Override
    public ReadStream<WebSocket> exceptionHandler(Handler<Throwable> hndlr) {
        return this;
    }

    @Override
    public ReadStream<WebSocket> handler(Handler<WebSocket> hndlr) {
        return this;
    }

    @Override
    public ReadStream<WebSocket> pause() {
        return this;
    }

    @Override
    public ReadStream<WebSocket> resume() {
        return this;
    }

    @Override
    public ReadStream<WebSocket> endHandler(Handler<Void> hndlr) {
        return this;
    }

    @Override
    public ReadStream<WebSocket> fetch(long l) {
        return this;
    }

    @Override
    public Pipe<WebSocket> pipe() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pipeTo(WriteStream<WebSocket> stream) {
    }

    @Override
    public void pipeTo(WriteStream<WebSocket> stream, Handler<AsyncResult<Void>> hndlr) {
    }

}
