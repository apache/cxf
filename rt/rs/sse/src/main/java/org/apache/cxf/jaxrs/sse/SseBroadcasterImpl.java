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
package org.apache.cxf.jaxrs.sse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.Flow;
import javax.ws.rs.Flow.Subscriber;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;

public class SseBroadcasterImpl implements SseBroadcaster {
    private final Map<Flow.Subscriber<? super OutboundSseEvent>, SseUnboundedSubscription> subscribers =
            new ConcurrentHashMap<>();

    private final Set<Consumer<Subscriber<? super OutboundSseEvent>>> closers =
            new CopyOnWriteArraySet<>();

    private final Set<BiConsumer<Subscriber<? super OutboundSseEvent>, Throwable>> exceptioners =
            new CopyOnWriteArraySet<>();

    @Override
    public void subscribe(Flow.Subscriber<? super OutboundSseEvent> subscriber) {
        try {
            if (!subscribers.containsKey(subscriber)) {
                final SseUnboundedSubscription subscription = new SseUnboundedSubscription(subscriber);
                if (subscribers.putIfAbsent(subscriber, subscription) == null) {
                    subscriber.onSubscribe(subscription);
                }
            }
        } catch (final Exception ex) {
            subscriber.onError(ex);
        }
    }

    @Override
    public void broadcast(OutboundSseEvent event) {
        for (Map.Entry<Flow.Subscriber<? super OutboundSseEvent>, SseUnboundedSubscription> entry 
            : subscribers.entrySet()) {
            try {
                entry.getValue().send(event);
            } catch (final Exception ex) {
                exceptioners.forEach(exceptioner -> exceptioner.accept(entry.getKey(), ex));
            }
        }
    }

    @Override
    public void onClose(Consumer<Subscriber<? super OutboundSseEvent>> subscriber) {
        closers.add(subscriber);
    }

    @Override
    public void onError(BiConsumer<Subscriber<? super OutboundSseEvent>, Throwable> exceptioner) {
        exceptioners.add(exceptioner);
    }

    @Override
    public void close() {
        subscribers.keySet().forEach(subscriber -> {
            subscriber.onComplete();
            closers.forEach(closer -> closer.accept(subscriber));
        });
    }
}
