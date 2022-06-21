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

package org.apache.cxf.systest.http_undertow.http2;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.transport.https.InsecureTrustManager;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;

/**
 * TODO: Use CXF client once https://issues.apache.org/jira/browse/CXF-8606 is dones
 */
public class Http2TestClient {
    private static final int BUFFER_SIZE = Integer.getInteger("test.bufferSize", 1024 * 16 - 20);
    private static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);
    
    private final UndertowClient client;
    private final UndertowXnioSsl ssl;
    private final XnioWorker worker;
    private final ByteBufferPool pool;
    
    public Http2TestClient(boolean secure) throws Exception {
        client = UndertowClient.getInstance();
        
        final Xnio xnio = Xnio.getInstance();
        worker = xnio.createWorker(null,  
            OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.WORKER_NAME, "TestClient")
                .getMap());
        
        pool = new DefaultByteBufferPool(true, BUFFER_SIZE, 1000, 10, 100);
        if (secure) {
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(new KeyManager[] {}, InsecureTrustManager.getNoOpX509TrustManagers(), null);
            ssl = new UndertowXnioSsl(xnio, OptionMap.EMPTY, sslContext);
        } else {
            ssl = null;
        }
    }
    
    public class RequestBuilder {
        private final String address;
        private String path = "";
        private String accept = MediaType.WILDCARD;
        private OptionMap options = OptionMap.EMPTY;

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
            options = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
            return this;
        }
        
        public ClientResponse get() throws IOException {
            return request(address, path, options, Methods.GET, accept);
        }
    }
    
    public RequestBuilder request(final String address) throws IOException {
        return new RequestBuilder(address);
    }

    public ClientResponse request(final String address, final String path, final OptionMap options,
            final HttpString method, final String accept) throws IOException {

        final ClientConnection connection = client
            .connect(URI.create(address), worker, ssl, pool, options)
            .get();
        
        try {
            final ClientRequest request = new ClientRequest()
                .setMethod(method)
                .setPath(path);

            request.getRequestHeaders().put(Headers.ACCEPT, accept);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            
            final CompletableFuture<ClientResponse> future = new CompletableFuture<ClientResponse>();
            connection.sendRequest(request, createClientCallback(future));
            return future.join();
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    private ClientCallback<ClientExchange> createClientCallback(final CompletableFuture<ClientResponse> future) {
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        new StringReadChannelListener(result.getConnection().getBufferPool()) {
                            @Override
                            protected void stringDone(String string) {
                                result.getResponse().putAttachment(RESPONSE_BODY, string);
                                future.complete(result.getResponse());
                            }

                            @Override
                            protected void error(IOException e) {
                                future.completeExceptionally(e);
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        future.completeExceptionally(e);
                    }
                });
            }

            @Override
            public void failed(IOException e) {
                future.completeExceptionally(e);
            }
        };
    }

    public String getBody(ClientResponse response) {
        return response.getAttachment(RESPONSE_BODY);
    }

}
