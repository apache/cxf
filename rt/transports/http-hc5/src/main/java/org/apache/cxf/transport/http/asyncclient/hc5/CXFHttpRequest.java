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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpEntity;

public class CXFHttpRequest extends HttpUriRequestBase implements Configurable {
    private static final long serialVersionUID = 1L;
    
    private HttpEntity entity;
    private AsyncWrappedOutputStreamBase out;
    private RequestConfig config;

    public CXFHttpRequest(String method, URI uri) {
        super(method, uri);
    }

    public void setOutputStream(AsyncWrappedOutputStreamBase o) {
        out = o;
    }
    public AsyncWrappedOutputStreamBase getOutputStream() {
        return out;
    }

    public HttpEntity getEntity() {
        return this.entity;
    }

    public void setEntity(final HttpEntity entity) {
        this.entity = entity;
    }
    
    @Override
    public RequestConfig getConfig() {
        return config;
    }

    public void setConfig(RequestConfig config) {
        this.config = config;
    }
    
    @Override
    public URI getUri() {
        try {
            return super.getUri();
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
