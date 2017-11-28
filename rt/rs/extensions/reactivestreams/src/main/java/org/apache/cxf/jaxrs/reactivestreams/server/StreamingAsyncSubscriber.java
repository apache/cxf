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
package org.apache.cxf.jaxrs.reactivestreams.server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.reactivestreams.Subscription;

public class StreamingAsyncSubscriber<T> extends AbstractSubscriber<T> {

    private BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
    private String openTag;
    private String closeTag;
    private String separator;
    private long pollTimeout;
    private long asyncTimeout;
    private volatile boolean completed;
    private AtomicBoolean firstWriteDone = new AtomicBoolean();
    public StreamingAsyncSubscriber(AsyncResponse ar, String openTag, String closeTag, String sep) {
        this(ar, openTag, closeTag, sep, 1000);
    }
    public StreamingAsyncSubscriber(AsyncResponse ar, String openTag, String closeTag, String sep,
                                    long pollTimeout) {
        this(ar, openTag, closeTag, sep, pollTimeout, 0);
    }
    public StreamingAsyncSubscriber(AsyncResponse ar, String openTag, String closeTag, String sep,
                                    long pollTimeout, long asyncTimeout) {
        super(ar);
        this.openTag = openTag;
        this.closeTag = closeTag;
        this.separator = sep;
        this.pollTimeout = pollTimeout;
        this.asyncTimeout = 0;
        if (asyncTimeout > 0) {
            ar.setTimeout(asyncTimeout, TimeUnit.MILLISECONDS);
            ar.setTimeoutHandler(new TimeoutHandlerImpl());
        }
    }
    @Override
    public void onSubscribe(Subscription subscription) {
        if (asyncTimeout == 0) {
            resumeAsyncResponse();
        }
        super.onSubscribe(subscription);
    }
    private void resumeAsyncResponse() {
        super.resume(new StreamingResponseImpl());
    }
    @Override
    public void onComplete() {
        completed = true;
    }

    @Override
    public void onNext(T bean) {
        if (asyncTimeout > 0 && getAsyncResponse().isSuspended()) {
            resumeAsyncResponse();
        }
        queue.add(bean);
        super.requestNext();
    }
    private class StreamingResponseImpl implements StreamingResponse<T> {

        @Override
        public void writeTo(Writer<T> writer) throws IOException {
            if (openTag != null) {
                writer.getEntityStream().write(StringUtils.toBytesUTF8(openTag));
            }
            while (!completed || !queue.isEmpty()) {
                try {
                    T bean = queue.poll(pollTimeout, TimeUnit.MILLISECONDS);
                    if (bean != null) {
                        if (firstWriteDone.getAndSet(true)) {
                            writer.getEntityStream().write(StringUtils.toBytesUTF8(separator));
                        }
                        writer.write(bean);
                    }
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            if (closeTag != null) {
                writer.getEntityStream().write(StringUtils.toBytesUTF8(closeTag));
            }

        }

    }
    public class TimeoutHandlerImpl implements TimeoutHandler {

        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            if (queue.isEmpty()) {
                asyncResponse.setTimeout(asyncTimeout, TimeUnit.MILLISECONDS);
            } else {
                resumeAsyncResponse();
            }

        }

    }
}
