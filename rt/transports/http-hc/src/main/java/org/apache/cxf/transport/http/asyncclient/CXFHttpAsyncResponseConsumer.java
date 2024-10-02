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

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

public class CXFHttpAsyncResponseConsumer implements HttpAsyncResponseConsumer<Boolean> {

    private final SharedInputBuffer buf;
    private final AsyncWrappedOutputStreamBase outstream;
    private final CXFResponseCallback responseCallback;

    private volatile boolean completed;
    private volatile Exception exception;
    private volatile HttpResponse response;

    public CXFHttpAsyncResponseConsumer(
            final AsyncWrappedOutputStreamBase asyncWrappedOutputStream,
            final SharedInputBuffer buf,
            final CXFResponseCallback responseCallback) {
        super();
        this.outstream = asyncWrappedOutputStream;
        this.responseCallback = responseCallback;
        this.buf = buf;
    }

    @Override
    public void close() throws IOException {
        buf.close();
    }

    @Override
    public boolean cancel() {
        completed = true;
        buf.shutdown();
        return true;
    }

    @Override
    public void responseReceived(final HttpResponse resp) throws IOException, HttpException {
        response = resp;
        responseCallback.responseReceived(response);
    }

    @Override
    public void consumeContent(final ContentDecoder dec, final IOControl ioc) throws IOException {
        // Only consume content when the work was accepted by the work queue
        if (outstream.retrySetHttpResponse(response)) {
            buf.consumeContent(dec, ioc);
        }
    }

    @Override
    public void responseCompleted(final HttpContext context) {
        completed = true;
        buf.close();
    }

    @Override
    public void failed(final Exception ex) {
        completed = true;
        exception = ex;
        buf.shutdown();
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Boolean getResult() {
        return exception != null;
    }

    @Override
    public boolean isDone() {
        return completed;
    }

}
