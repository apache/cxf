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

import java.util.List;
import java.util.concurrent.CancellationException;

import jakarta.ws.rs.container.AsyncResponse;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractSubscriber<T> implements Subscriber<T> {

    private AsyncResponse ar;
    private Subscription subscription;

    protected AbstractSubscriber(AsyncResponse ar) {
        this.ar = ar;
    }
    public void resume(T response) {
        ar.resume(response);
    }

    public void resume(List<T> response) {
        ar.resume(response);
    }

    public void resume(StreamingResponse<T> response) {
        ar.resume(response);
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof CancellationException) {
            ar.cancel();
        } else {
            ar.resume(t);
        }
    }

    @Override
    public void onSubscribe(Subscription inSubscription) {
        this.subscription = inSubscription;
        requestAll();
    }

    @Override
    public void onNext(T t) {
        resume(t);
    }

    @Override
    public void onComplete() {
    }

    protected AsyncResponse getAsyncResponse() {
        return ar;
    }

    protected Subscription getSubscription() {
        return subscription;
    }

    protected void requestNext() {
        request(1);
    }

    protected void requestAll() {
        request(Long.MAX_VALUE);
    }

    protected final void request(long elements) {
        this.subscription.request(elements);
    }
}
