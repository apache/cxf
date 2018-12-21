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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.LongAdder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;

import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SseBroadcasterImplTest {
    private SseBroadcaster broadcaster;
    private MessageBodyWriter<OutboundSseEvent> writer;
    private MockHttpServletResponse response;
    private MockAsyncContext ctx;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        broadcaster = new SseBroadcasterImpl();
        response = new MockHttpServletResponse();
        writer = mock(MessageBodyWriter.class);
        ctx = new MockAsyncContext(new MockHttpServletRequest(), response);
    }

    @Test
    public void testOnCloseCallbackIsCalled() {
        final LongAdder adder = new LongAdder();
        final SseEventSinkImpl sink = new SseEventSinkImpl(writer, null, ctx);
        broadcaster.register(sink);

        broadcaster.onClose(s -> {
            if (s == sink) {
                adder.increment();
            }
        });
        assertThat(adder.intValue(), equalTo(0));

        sink.close();
        assertThat(adder.intValue(), equalTo(1));
    }

    @Test
    public void testOnCloseCallbackIsCalledForBroadcaster() {
        final LongAdder adder = new LongAdder();
        final SseEventSinkImpl sink = new SseEventSinkImpl(writer, null, ctx);
        broadcaster.register(sink);

        broadcaster.onClose(s -> {
            if (s == sink) {
                adder.increment();
            }
        });
        assertThat(adder.intValue(), equalTo(0));

        broadcaster.close();
        assertThat(adder.intValue(), equalTo(1));
    }

    @Test
    public void testOnErrorCallbackIsCalled() throws WebApplicationException, IOException {
        when(writer.isWriteable(any(), any(), any(), any())).thenReturn(true);

        final LongAdder adder = new LongAdder();
        final SseEventSinkImpl sink = new SseEventSinkImpl(writer, null, ctx) {
            @Override
            public CompletionStage<?> send(OutboundSseEvent event) {
                ctx.start(() -> {
                    throw new RuntimeException("Failed to schedule async task");
                });
                return CompletableFuture.completedFuture(null);
            }
        };
        broadcaster.register(sink);

        broadcaster.onError((s, ex) -> {
            if (s == sink) {
                adder.increment();
            }
        });
        assertThat(adder.intValue(), equalTo(0));

        broadcaster.broadcast(new OutboundSseEventImpl.BuilderImpl().build());
        broadcaster.close();

        assertThat(adder.intValue(), equalTo(1));
    }
}
