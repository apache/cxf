/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.http2_jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * TODO: Use CXF client once https://issues.apache.org/jira/browse/CXF-8606 is dones
 */
public class Http2TestClient implements AutoCloseable {
    private final HTTP2Client client;
    private final SslContextFactory sslContextFactory;
    
    public Http2TestClient(boolean secure) throws Exception {
        client = new HTTP2Client();
        if (secure) {
            sslContextFactory = new SslContextFactory.Client(true);
            client.addBean(sslContextFactory);
        } else {
            sslContextFactory = null;
        }
        client.start();
    }
    
    public static class ClientResponse {
        private String body;
        private HttpVersion protocol;
        private int responseCode;

        public void setBody(String body) {
            this.body = body;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setResponseCode(int rc) {
            this.responseCode = rc;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public HttpVersion getProtocol() {
            return protocol;
        }
        
        public void setProtocol(HttpVersion protocol) {
            this.protocol = protocol;
        }
    }
    
    public class RequestBuilder {
        private final String address;
        private String path = "";
        private String accept = MediaType.WILDCARD;
        private HttpVersion version = HttpVersion.HTTP_1_1;

        public RequestBuilder(final String address) {
            this.address = address;
        }
        
        public RequestBuilder path(final String p) {
            this.path = p;
            return this;
        }
        
        
        public RequestBuilder accept(final String a) {
            this.accept = a;
            return this;
        }
        
        public RequestBuilder http2() {
            version = HttpVersion.HTTP_2;
            return this;
        }
        
        public ClientResponse get() throws InterruptedException, ExecutionException, TimeoutException {
            return request(address, path, version, "GET", accept);
        }
    }
    
    public RequestBuilder request(final String address) throws IOException {
        return new RequestBuilder(address);
    }

    public ClientResponse request(final String address, final String path, 
            final HttpVersion version, final String method, final String accept) 
                throws InterruptedException, ExecutionException, TimeoutException {

        final URI uri = URI.create(address);
        final FuturePromise<Session> sessionPromise = new FuturePromise<>();

        client.connect(sslContextFactory, new InetSocketAddress(uri.getHost(), uri.getPort()), 
            new ServerSessionListener.Adapter(), sessionPromise);
        final Session session = sessionPromise.get();

        final HttpFields.Mutable requestFields = HttpFields.build();
        requestFields.add(HttpHeader.ACCEPT, accept);
        requestFields.add(HttpHeader.HOST, "localhost");

        final MetaData.Request request = new MetaData.Request(method, HttpURI.build(address + path), 
            version, requestFields);

        final CompletableFuture<ClientResponse> future = new CompletableFuture<>();
        final Stream.Listener responseListener = new ResponseListener(future);
        
        final HeadersFrame headersFrame = new HeadersFrame(request, null, true);
        session.newStream(headersFrame, new FuturePromise<>(), responseListener);
        return future.get(5, TimeUnit.SECONDS);
    }
    
    @Override
    public void close() throws Exception {
        client.stop();
    }
    
    private final class ResponseListener extends Stream.Listener.Adapter {
        private final ClientResponse response = new ClientResponse();
        private final CompletableFuture<ClientResponse> future;
        
        ResponseListener(final CompletableFuture<ClientResponse> f) {
            this.future = f;
        }
        
        @Override
        public void onHeaders(Stream stream, HeadersFrame frame) {
            final MetaData metaData = frame.getMetaData();
            response.setProtocol(metaData.getHttpVersion());
            if (metaData.isResponse()) {
                final int status = ((MetaData.Response)metaData).getStatus();
                response.setResponseCode(status);
                // Unsuccessful response
                if (status >= 400) {
                    future.complete(response);
                }
            }
            super.onHeaders(stream, frame);
        }
        
        @Override
        public void onData(Stream stream, DataFrame frame, Callback callback) {
            byte[] bytes = new byte[frame.getData().remaining()];
            frame.getData().get(bytes);
            response.setBody(new String(bytes));
            future.complete(response);
            super.onData(stream, frame, callback);
        }
        
        @Override
        public boolean onIdleTimeout(Stream stream, Throwable x) {
            future.completeExceptionally(x);
            return super.onIdleTimeout(stream, x);
        }
        
        @Override
        public void onFailure(Stream stream, int error, String reason, Throwable failure, Callback callback) {
            future.completeExceptionally(new ClientErrorException(reason, error));
            super.onFailure(stream, error, reason, failure, callback);
        }
    }
}
