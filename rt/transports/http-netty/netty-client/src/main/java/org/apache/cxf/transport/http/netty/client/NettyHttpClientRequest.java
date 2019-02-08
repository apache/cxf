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

package org.apache.cxf.transport.http.netty.client;

import java.net.URI;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;

public class NettyHttpClientRequest {

    private HttpRequest request;
    private HttpResponse response;
    private URI uri;
    private String method;
    private CxfResponseCallBack cxfResponseCallback;
    private int connectionTimeout;
    private int receiveTimeout;

    public NettyHttpClientRequest(URI requestUri, String method) {
        this.uri = requestUri;
        this.method = method;
    }

    public void createRequest(ByteBuf content) {
        this.request  =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                       HttpMethod.valueOf(method),
                                       uri.getPath().toString(), content);
        // setup the default headers
        request.headers().set("Connection", "keep-alive");
        request.headers().set("Host", uri.getHost() + ":"
            + (uri.getPort() != -1 ? uri.getPort() : "http".equals(uri.getScheme()) ? 80 : 443));
    }

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public URI getUri() {
        return uri;
    }

    public void setCxfResponseCallback(CxfResponseCallBack callback) {
        this.cxfResponseCallback = callback;
    }

    public CxfResponseCallBack getCxfResponseCallback() {
        return cxfResponseCallback;
    }

    public void setConnectionTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setReceiveTimeout(int timeout) {
        this.receiveTimeout = timeout;
    }

    public int getReceiveTimeout() {
        return receiveTimeout;
    }


}
