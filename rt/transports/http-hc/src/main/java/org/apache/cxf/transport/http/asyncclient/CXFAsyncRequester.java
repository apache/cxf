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

package org.apache.cxf.transport.http.asyncclient;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpVersion;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.conn.ClientAsyncConnectionManager;
import org.apache.http.nio.conn.scheme.AsyncScheme;
import org.apache.http.nio.conn.scheme.AsyncSchemeRegistry;
import org.apache.http.nio.conn.ssl.SSLLayeringStrategy;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpContext;

public class CXFAsyncRequester {

    private final ClientAsyncConnectionManager caConMan;
    
    public CXFAsyncRequester(
            ClientAsyncConnectionManager caConMan) {
        super();
        this.caConMan = caConMan;
    }

    public <T> Future<T> execute(
            final AsyncHTTPConduit conduit,
            final URI uri,
            final long connectionTimeout,
            final HttpAsyncRequestProducer requestProducer,
            final HttpAsyncResponseConsumer<T> responseConsumer,
            final HttpContext context,
            final FutureCallback<T> callback) {
        if (requestProducer == null) {
            throw new IllegalArgumentException("HTTP request producer may not be null");
        }
        if (responseConsumer == null) {
            throw new IllegalArgumentException("HTTP response consumer may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        BasicFuture<T> future = new BasicFuture<T>(callback);
        final AsyncSchemeRegistry reg = new AsyncSchemeRegistry();
        reg.register(new AsyncScheme("http", 80, null));
               
        if ("https".equals(uri.getScheme())) {
            try {
                final SSLContext sslcontext = conduit.getSSLContext();
                reg.register(new AsyncScheme("https", 443, new SSLLayeringStrategy(sslcontext) {
                    @Override
                    protected void initializeEngine(SSLEngine engine) {
                        conduit.initializeSSLEngine(sslcontext, engine);
                    }
                    @Override
                    protected void verifySession(final IOSession iosession,
                                          final SSLSession sslsession) throws SSLException {
                        super.verifySession(iosession, sslsession);
                        iosession.setAttribute("cxf.handshake.done", Boolean.TRUE);
                        CXFHttpRequest req = (CXFHttpRequest)iosession
                            .removeAttribute(CXFHttpRequest.class.getName());
                        if (req != null) {
                            req.getOutputStream().setSSLSession(sslsession);
                        }
                    }
                }));
            } catch (GeneralSecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        DefaultHttpAsyncClient dhac = new DefaultHttpAsyncClient(caConMan) {
            @Override
            protected HttpParams createHttpParams() {
                HttpParams params = new SyncBasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpConnectionParams.setTcpNoDelay(params, true);
                HttpConnectionParams.setSocketBufferSize(params, 16332);
                return params;
            }
        };
        context.setAttribute(ClientContext.SCHEME_REGISTRY, reg);
        dhac.execute(requestProducer, responseConsumer, context, callback);

        return future;
    }
    
    
    
}
