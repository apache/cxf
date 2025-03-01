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


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.HTTP;

public class CXFHttpRequest extends AbstractHttpMessage implements HttpEntityEnclosingRequest, Configurable {

    private final String method;

    private URI uri;
    private HttpEntity entity;
    private AsyncWrappedOutputStreamBase out;
    private RequestConfig config;

    public CXFHttpRequest(final String method) {
        super();
        this.method = method;
    }

    public void setOutputStream(AsyncWrappedOutputStreamBase o) {
        out = o;
    }
    public AsyncWrappedOutputStreamBase getOutputStream() {
        return out;
    }
    public URI getURI() {
        return uri;
    }

    public void setURI(final URI u) {
        this.uri = u;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public RequestLine getRequestLine() {
        return new BasicRequestLine(
                method,
                uri != null ? uri.toASCIIString() : "/",
                HttpVersion.HTTP_1_1);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return HttpVersion.HTTP_1_1;
    }

    public HttpEntity getEntity() {
        return this.entity;
    }

    public void setEntity(final HttpEntity entity) {
        this.entity = entity;
    }

    public boolean expectContinue() {
        Header expect = getFirstHeader(HTTP.EXPECT_DIRECTIVE);
        return expect != null && HTTP.EXPECT_CONTINUE.equalsIgnoreCase(expect.getValue());
    }

    @Override
    public RequestConfig getConfig() {
        return config;
    }

    public void setConfig(RequestConfig config) {
        this.config = config;
    }

}
