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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

public class CXFHttpAsyncResponseConsumer implements AsyncResponseConsumer<Boolean> {
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
    public void releaseResources() {
        buf.close();
    }
    
    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        capacityChannel.update(Integer.MAX_VALUE);
    }
    
    @Override
    public void consumeResponse(HttpResponse resp, EntityDetails entityDetails, HttpContext context, 
            FutureCallback<Boolean> resultCallback) throws HttpException, IOException {
        response = resp;
        responseCallback.responseReceived(response);
        resultCallback.completed(true);
    }

    @Override
    public void consume(ByteBuffer src) throws IOException {
        // Replicating HttpClient 4.x behavior. Try to gently feed more
        // data to the event dispatcher if the session input buffer has 
        // not been fully exhausted (the choice of 5 iterations is purely arbitrary)
        // or work queue is not ready to process the response.
        for (int i = 0; i < 5; i++) {
            // Only consume content when the work was accepted by the work queue
            if (outstream.retrySetHttpResponse(response)) {
                buf.consumeContent(src, completed);
                break;
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public void failed(final Exception ex) {
        completed = true;
        exception = ex;
        buf.shutdown();
    }

    @Override
    public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
        completed = true;
        buf.close();
    }

    @Override
    public void informationResponse(HttpResponse resp, HttpContext context) throws HttpException, IOException {
    }
    
    public Exception getException() {
        return exception;
    }
    
    public boolean isCompleted() {
        return completed;
    }
}
