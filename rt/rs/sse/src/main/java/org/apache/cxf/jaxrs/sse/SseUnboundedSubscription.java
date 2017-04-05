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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.Flow;
import javax.ws.rs.Flow.Subscription;
import javax.ws.rs.sse.OutboundSseEvent;

class SseUnboundedSubscription implements Subscription {
    // Has subscription been cancelled or not?
    private boolean cancelled;
    // Current demand: what has been requested but not yet delivered
    private long demand;
    private final BlockingQueue<OutboundSseEvent> buffer = new LinkedBlockingQueue<>(); 
    private final Flow.Subscriber<? super OutboundSseEvent> subscriber;
    
    SseUnboundedSubscription(Flow.Subscriber<? super OutboundSseEvent> subscriber) {
        this.subscriber = subscriber;
    }
    
    public void request(long n) {
        if (demand + n < 1) {
            // Effectively unbounded demand 
            demand = Long.MAX_VALUE;
            send();
        } else {
            // Here we record the downstream demand
            demand += n;
            send();
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
    
    public void send(OutboundSseEvent event) throws InterruptedException {
        if (!cancelled && buffer.offer(event)) {
            send();
        }
    }
    
    private void send() {
        while (!cancelled && demand > 0 && !buffer.isEmpty()) {
            final OutboundSseEvent event = buffer.poll();
            if (event != null) {
                subscriber.onNext(event);
                --demand;
            }
        }
    }
}
