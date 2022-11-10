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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SseEventSinkContextProviderTest {
    private static final OutboundSseEvent EVENT = new OutboundSseEventImpl.BuilderImpl().build();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private SseEventSinkContextProvider provider;
    private Message message;

    @Before
    public void setUp() {
        provider = new SseEventSinkContextProvider();
        
        final Exchange exchange = mock(Exchange.class);
        final Endpoint endpoint = mock(Endpoint.class);
        final ContinuationProvider continuationProvider = mock(ContinuationProvider.class);
        
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final AsyncContext ctx = new TestAsyncContext(request, response) {
            @Override
            public void start(Runnable runnable) {
                /* do nothing */
            } 
        };
        
        when(request.getAsyncContext()).thenReturn(ctx);
        
        message = new MessageImpl();
        message.setExchange(exchange);
        message.put(ContinuationProvider.class.getName(), continuationProvider);
        message.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        
        when(exchange.getEndpoint()).thenReturn(endpoint);
    }

    @Test
    public void testCreateSseEventSinkWithDefaultBufferSize() {
        final SseEventSink sink = provider.createContext(message);
        
        IntStream
            .range(0, 10000)
            .mapToObj(i -> sink.send(EVENT))
            .map(CompletionStage::toCompletableFuture)
            // At this point, buffer is full, but nothing has been delivered so far
            .forEach(f -> assertThat(f.isDone(), equalTo(false)));
       
        // The buffer overflow should trigger message rejection and exceptional completion
        final CompletableFuture<?> overflow = sink.send(EVENT).toCompletableFuture();
        assertThat(overflow.isCompletedExceptionally(), equalTo(true));
        
        exception.expect(CompletionException.class);
        exception.expectMessage("The buffer is full (10000), unable to queue SSE event for send.");

        overflow.join();
    }
    
    @Test
    public void testCreateSseEventSinkWithCustomBufferSize() {
        message.put(SseEventSinkImpl.BUFFER_SIZE_PROPERTY, 20000);
        final SseEventSink sink = provider.createContext(message);
        
        IntStream
            .range(0, 20000)
            .mapToObj(i -> sink.send(EVENT))
            .map(CompletionStage::toCompletableFuture)
            // At this point, buffer is full, but nothing has been delivered so far
            .forEach(f -> assertThat(f.isDone(), equalTo(false)));
       
        // The buffer overflow should trigger message rejection and exceptional completion
        final CompletableFuture<?> overflow = sink.send(EVENT).toCompletableFuture();
        assertThat(overflow.isCompletedExceptionally(), equalTo(true));
        
        exception.expect(CompletionException.class);
        exception.expectMessage("The buffer is full (20000), unable to queue SSE event for send."); 

        overflow.join();
    }
}
