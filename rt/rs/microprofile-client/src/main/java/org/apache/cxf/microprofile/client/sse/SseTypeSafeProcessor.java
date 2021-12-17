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
package org.apache.cxf.microprofile.client.sse;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.sse.InboundSseEvent;
import org.apache.cxf.common.logging.LogUtils;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SseTypeSafeProcessor<T> implements Processor<InboundSseEvent, T> {
    private static final Logger LOG = LogUtils.getL7dLogger(SseTypeSafeProcessor.class);

    private SseSubscription incomingSubscription;
    private final SsePublisher ssePublisher;
    private final List<Subscriber<? super T>> subscribers = new LinkedList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final GenericType<?> type;
    private final AtomicBoolean isSubscribed = new AtomicBoolean();

    SseTypeSafeProcessor(GenericType<T> type, SsePublisher ssePublisher) {
        this.type = type;
        this.ssePublisher = ssePublisher;
    }

    @Override
    public void onSubscribe(Subscription s) {
        incomingSubscription = (SseSubscription) s;
        LOG.finest("onSubscribe " + s);
    }

    @Override
    public void onNext(InboundSseEvent t) {
        LOG.entering(SseTypeSafeProcessor.class.getName(), "onNext", t);
        if (incomingSubscription == null) {
            throw new IllegalStateException("not subscribed");
        }
        if (!isClosed.get()) {
            @SuppressWarnings("unchecked")
            T data = (T) t.readData(type);
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onNext(data);
            }
        }
        LOG.exiting(SseTypeSafeProcessor.class.getName(), "onNext");
    }

    @Override
    public void onError(Throwable t) {
        if (isClosed.compareAndSet(false, true)) {
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onError(t);
            }
        }
    }

    @Override
    public void onComplete() {
        if (isClosed.compareAndSet(false, true)) {
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onComplete();
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        LOG.finest("subscribe " + s);
        subscribers.add(s);
        if (isSubscribed.compareAndSet(false, true)) {
            ssePublisher.subscribe(this);
        }
        if (incomingSubscription == null) {
            throw new IllegalStateException();
        }
        s.onSubscribe(new SseTypeSafeSubscription(incomingSubscription));
    }
}