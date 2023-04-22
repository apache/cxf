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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.InboundSseEvent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class SsePublisher implements Publisher<InboundSseEvent> {

    final Executor executor;
    final BufferedReader br;
    final Providers providers;
    final List<SseSubscription> subscriptions = new CopyOnWriteArrayList<>();
    final AtomicBoolean isStarted = new AtomicBoolean(false);

    SsePublisher(InputStream is, Executor executor, Providers providers) {
        br = new BufferedReader(new InputStreamReader(is));
        this.executor = executor;
        this.providers = providers;
    }

    @Override
    public void subscribe(Subscriber<? super InboundSseEvent> subscriber) {
        SseSubscription subscription = new SseSubscription(this, subscriber);
        subscriptions.add(subscription);
        subscription.fireSubscribe();
        start();
    }

    private void start() {
        if (isStarted.compareAndSet(false, true)) {
            executor.execute(() -> {
                try (BufferedReader br2 = br) {
                    SseEventBuilder builder = new SseEventBuilder(providers);
                    String line = br.readLine();
                    while (line != null && !subscriptions.isEmpty()) {
                        if (line.startsWith("data:")) {
                            builder.data(removeSpace(line.substring(5)));
                        } else if (line.startsWith("id:")) {
                            builder.id(removeSpace(line.substring(3)));
                        } else if (line.startsWith("event:")) {
                            builder.name(removeSpace(line.substring(6)));
                        } else if (line.startsWith(":")) {
                            builder.comment(removeSpace(line.substring(1)));
                        } else if ("".equals(line)) {
                            InboundSseEvent event = builder.build();
                            for (SseSubscription subscription : subscriptions) {
                                subscription.fireEvent(event);
                            }
                            builder = new SseEventBuilder(providers);
                        }
                        line = br.readLine();
                    }
                    for (SseSubscription subscription : subscriptions) {
                        subscription.complete();
                    }
                } catch (IOException ex) {
                    for (SseSubscription subscription : subscriptions) {
                        subscription.fireError(ex);
                    }
                }
            });
        }
    }

    void removeSubscription(SseSubscription subscription) {
        subscriptions.remove(subscription);
    }

    private String removeSpace(String s) {
        if (s != null && s.startsWith(" ")) {
            return s.substring(1);
        }
        return s;
    }
}