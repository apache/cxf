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

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

public class CXFHttpAsyncRequestProducer implements HttpAsyncRequestProducer {

    private final CXFHttpRequest request;
    private final SharedOutputBuffer buf;
    
    public CXFHttpAsyncRequestProducer(final CXFHttpRequest request, final SharedOutputBuffer buf) {
        super();
        this.buf = buf;
        this.request = request;
    }
    
    public HttpHost getTarget() {
        URI uri = request.getURI();
        if (uri == null) {
            throw new IllegalStateException("Request URI is null");
        }
        if (!uri.isAbsolute()) {
            throw new IllegalStateException("Request URI is not absolute");
        }
        int i = uri.getPort();
        if (i == -1) {
            i = 80;
        }
        return new HttpHost(uri.getHost(), i, uri.getScheme());
    }
    
    public HttpRequest generateRequest() throws IOException, HttpException {
        return request;
    }
    
    public void produceContent(final ContentEncoder enc, final IOControl ioc) throws IOException {
        buf.produceContent(enc, ioc);
    }
    
    public void requestCompleted(final HttpContext context) {
    }
    
    public void failed(final Exception ex) {
        buf.shutdown();
    }
    
    public boolean isRepeatable() {
        return false;
    }
    
    public void resetRequest() throws IOException {
    }

    @Override
    public void close() throws IOException {
        buf.close();
    }
    
}
