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

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SseSubscriptionTest {

    @Test
    public void testRequestNWithDefaultBuffer() throws Exception {
        MockSubscriber mockSubscriber = new MockSubscriber();
        SseSubscription sseSubscription = new SseSubscription(null, mockSubscriber);

        sseSubscription.fireSubscribe();
        assertEquals(sseSubscription, mockSubscriber.subscription);

        sseSubscription.fireEvent(new MockInboundSseEvent(1));
        sseSubscription.fireEvent(new MockInboundSseEvent(2));

        sseSubscription.request(2);
        assertEquals(2, mockSubscriber.receivedEvents.size());
        assertEquals("1", mockSubscriber.receivedEvents.get(0).readData());
        assertEquals("2", mockSubscriber.receivedEvents.get(1).readData());

        sseSubscription.request(1);
        sseSubscription.fireEvent(new MockInboundSseEvent(3));
        sseSubscription.fireEvent(new MockInboundSseEvent(4));
        assertEquals(3, mockSubscriber.receivedEvents.size());
        assertEquals("3", mockSubscriber.receivedEvents.get(2).readData());
    }

    @Test
    public void testRequestNWithNoBuffer() throws Exception {
        MockSubscriber mockSubscriber = new MockSubscriber();
        SseSubscription sseSubscription = new SseSubscription(null, mockSubscriber);
        sseSubscription.setBufferSize(0);

        sseSubscription.fireSubscribe();
        assertEquals(sseSubscription, mockSubscriber.subscription);

        sseSubscription.fireEvent(new MockInboundSseEvent(1));
        sseSubscription.fireEvent(new MockInboundSseEvent(2));

        sseSubscription.request(2);
        assertEquals(0, mockSubscriber.receivedEvents.size());

        sseSubscription.fireEvent(new MockInboundSseEvent(3));
        sseSubscription.fireEvent(new MockInboundSseEvent(4));
        sseSubscription.fireEvent(new MockInboundSseEvent(5));
        assertEquals(2, mockSubscriber.receivedEvents.size());
        assertEquals("3", mockSubscriber.receivedEvents.get(0).readData());
        assertEquals("4", mockSubscriber.receivedEvents.get(1).readData());
    }

    @Test
    public void testBufferOverlowsFIFO() throws Exception {
        MockSubscriber mockSubscriber = new MockSubscriber();
        SseSubscription sseSubscription = new SseSubscription(null, mockSubscriber);
        sseSubscription.setBufferSize(8);
        for (int i = 0; i < 64; i++) {
            sseSubscription.fireEvent(new MockInboundSseEvent(i));
        }
        sseSubscription.request(2);
        assertEquals(2, mockSubscriber.receivedEvents.size());
        assertEquals("56", mockSubscriber.receivedEvents.get(0).readData());
        assertEquals("57", mockSubscriber.receivedEvents.get(1).readData());

    }

    class MockSubscriber implements Subscriber<InboundSseEvent> {

        Subscription subscription;
        List<InboundSseEvent> receivedEvents = new ArrayList<>();
        Throwable throwable;
        boolean complete;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(InboundSseEvent t) {
            receivedEvents.add(t);
        }

        @Override
        public void onError(Throwable t) {
            throwable = t;
        }

        @Override
        public void onComplete() {
            complete = true;
        }
    }
    class MockInboundSseEvent implements InboundSseEvent {

        String data;

        MockInboundSseEvent(int i) {
            data = Integer.toString(i);
        }

        @Override
        public String getComment() {
            // no-op
            return null;
        }

        @Override
        public String getId() {
            // no-op
            return null;
        }

        @Override
        public String getName() {
            // no-op
            return null;
        }

        @Override
        public long getReconnectDelay() {
            // no-op
            return 0;
        }

        @Override
        public boolean isReconnectDelaySet() {
            // no-op
            return false;
        }

        @Override
        public boolean isEmpty() {
            // no-op
            return false;
        }

        @Override
        public String readData() {
            return data;
        }

        @Override
        public <T> T readData(Class<T> arg0) {
            // no-op
            return null;
        }

        @Override
        public <T> T readData(GenericType<T> arg0) {
            // no-op
            return null;
        }

        @Override
        public <T> T readData(Class<T> arg0, MediaType arg1) {
            // no-op
            return null;
        }

        @Override
        public <T> T readData(GenericType<T> arg0, MediaType arg1) {
            // no-op
            return null;
        }
    }
}