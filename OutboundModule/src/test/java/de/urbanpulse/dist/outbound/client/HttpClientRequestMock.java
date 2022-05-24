package de.urbanpulse.dist.outbound.client;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HttpClientRequestMock implements HttpClientRequest {

    private boolean endCalled;
    private MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    public boolean isEndCalled() {
        return endCalled;
    }


    @Override
    public HttpClientRequest exceptionHandler(Handler<Throwable> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest write(Buffer buffer) {
        return this;
    }

    @Override
    public HttpClientRequest setWriteQueueMaxSize(int i) {
        return this;
    }

    @Override
    public HttpClientRequest drainHandler(Handler<Void> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest handler(Handler<HttpClientResponse> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest pause() {
        return this;
    }

    @Override
    public HttpClientRequest resume() {
        return this;
    }

    @Override
    public HttpClientRequest endHandler(Handler<Void> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest setChunked(boolean bln) {
        return this;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public HttpMethod method() {
        return null;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public MultiMap headers() {
        return headers;
    }

    @Override
    public HttpClientRequest putHeader(String string, String string1) {
        headers.add(string, string1);
        return this;
    }

    @Override
    public HttpClientRequest putHeader(CharSequence cs, CharSequence cs1) {
        headers.add(cs, cs1);
        return this;
    }

    @Override
    public HttpClientRequest putHeader(String string, Iterable<String> itrbl) {
        headers.add(string, itrbl);
        return this;
    }

    @Override
    public HttpClientRequest putHeader(CharSequence cs, Iterable<CharSequence> itrbl) {
        headers.add(cs, itrbl);
        return this;
    }

    @Override
    public HttpClientRequest write(String string) {
        return this;
    }

    @Override
    public HttpClientRequest write(String string, String string1) {
        return this;
    }

    @Override
    public HttpClientRequest continueHandler(Handler<Void> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest sendHead() {
        return this;
    }

    @Override
    public void end(String string) {
        endCalled = true;
    }

    @Override
    public void end(String string, String string1) {
        endCalled = true;
    }

    @Override
    public void end(Buffer buffer) {
        endCalled = true;
    }

    @Override
    public void end() {
        endCalled = true;
    }

    @Override
    public HttpClientRequest setTimeout(long l) {
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public HttpClientRequest setFollowRedirects(boolean arg0) {
        return this;
    }

    @Override
    public String getRawMethod() {
        return null;
    }

    @Override
    public HttpClientRequest setRawMethod(String method) {
        return this;
    }

    @Override
    public String absoluteURI() {
        return null;
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    public HttpClientRequest setHost(String host) {
        return this;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public HttpClientRequest sendHead(Handler<HttpVersion> completionHandler) {
        return this;
    }

    @Override
    public HttpClientRequest pushHandler(Handler<HttpClientRequest> handler) {
        return this;
    }

    @Override
    public boolean reset(long code) {
        return true;
    }

    @Override
    public HttpConnection connection() {
        return null;
    }

    @Override
    public HttpClientRequest connectionHandler(Handler<HttpConnection> handler) {
        return this;
    }

    @Override
    public HttpClientRequest writeCustomFrame(int type, int flags, Buffer payload) {
        return this;
    }

    @Override
    public HttpClientRequest write(Buffer buffer, Handler<AsyncResult<Void>> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest fetch(long l) {
        return this;
    }

    @Override
    public HttpClientRequest setMaxRedirects(int i) {
        return this;
    }

    @Override
    public HttpClientRequest write(String string, Handler<AsyncResult<Void>> hndlr) {
        return this;
    }

    @Override
    public HttpClientRequest write(String string, String string1, Handler<AsyncResult<Void>> hndlr) {
        return this;
    }

    @Override
    public void end(String string, Handler<AsyncResult<Void>> hndlr) {
    }

    @Override
    public void end(String string, String string1, Handler<AsyncResult<Void>> hndlr) {
    }

    @Override
    public void end(Buffer buffer, Handler<AsyncResult<Void>> hndlr) {
    }

    @Override
    public void end(Handler<AsyncResult<Void>> hndlr) {
    }

    @Override
    public boolean reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int streamId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HttpClientRequest writeCustomFrame(HttpFrame hf) {
        return this;
    }

    @Override
    public HttpClientRequest setStreamPriority(StreamPriority sp) {
        return this;
    }

    @Override
    public StreamPriority getStreamPriority() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Pipe<HttpClientResponse> pipe() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pipeTo(WriteStream<HttpClientResponse> dst) {
    }

    @Override
    public void pipeTo(WriteStream<HttpClientResponse> dst, Handler<AsyncResult<Void>> handler) {
    }

}
