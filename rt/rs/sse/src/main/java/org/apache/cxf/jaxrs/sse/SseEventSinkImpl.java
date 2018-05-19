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

import java.lang.annotation.Annotation;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.common.logging.LogUtils;

public class SseEventSinkImpl implements SseEventSink {
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation [] {};
    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSinkImpl.class);
    private static final int BUFFER_SIZE = 10000; // buffering 10000 messages

    private final AsyncContext ctx;
    private final MessageBodyWriter<OutboundSseEvent> writer;
    private final Queue<QueuedEvent> buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean dispatching = new AtomicBoolean(false);

    public SseEventSinkImpl(final MessageBodyWriter<OutboundSseEvent> writer, 
            final AsyncResponse async, final AsyncContext ctx) {
        
        this.writer = writer;
        this.buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
        this.ctx = ctx;

        if (ctx == null) {
            throw new IllegalStateException("Unable to retrieve the AsyncContext for this request. "
                + "Is the Servlet configured properly?");
        }

        ctx.getResponse().setContentType(OutboundSseEventBodyWriter.SERVER_SENT_EVENTS);
    }

    public AsyncContext getAsyncContext() {
        return ctx;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // In case we are still dispatching, give the events the chance to be
            // sent over to the consumers. The good example would be sent(event) call,
            // immediately followed by the close() call.
            if (!awaitQueueToDrain(5, TimeUnit.SECONDS)) {
                LOG.warning("There are still SSE events the queue which may not be delivered (closing now)");
            }
            
            try {
                ctx.complete();
            } catch (final IllegalStateException ex) {
                LOG.warning("Failed to close the AsyncContext cleanly: " + ex.getMessage());
            }
        }
    }

    private boolean awaitQueueToDrain(int timeout, TimeUnit unit) {
        final long parkTime = unit.toNanos(timeout) / 20;
        int attempt = 0;
        
        while (dispatching.get() && ++attempt < 20) {
            LockSupport.parkNanos(parkTime);
        }
        
        return buffer.isEmpty();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        final CompletableFuture<?> future = new CompletableFuture<>();

        if (!closed.get() && writer != null) {
            if (buffer.offer(new QueuedEvent(event, future))) {
                if (dispatching.compareAndSet(false, true)) {
                    ctx.start(this::dequeue);
                }
            } else {
                future.completeExceptionally(new IllegalStateException(
                    "The buffer is full (10000), unable to queue SSE event for send"));
            }
        } else {
            future.completeExceptionally(new IllegalStateException(
                "The sink is already closed, unable to queue SSE event for send"));
        }

        return future;
    }

    private void dequeue() {
        try {
            while (true) {
                final QueuedEvent qeuedEvent = buffer.poll();
                
                // Nothing queued, release the thread
                if (qeuedEvent == null) {
                    break;
                }
                
                final OutboundSseEvent event = qeuedEvent.event;
                final CompletableFuture<?> future = qeuedEvent.completion;
    
                try {
                    writer.writeTo(event, event.getClass(), event.getGenericType(), EMPTY_ANNOTATIONS,
                        event.getMediaType(), null, ctx.getResponse().getOutputStream());
                    ctx.getResponse().flushBuffer();
                    future.complete(null);
                } catch (final Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        } finally {
            dispatching.set(false);
        }
    }

    private static class QueuedEvent {
        private final OutboundSseEvent event;
        private final CompletableFuture<?> completion;

        QueuedEvent(OutboundSseEvent event, CompletableFuture<?> completion) {
            this.event = event;
            this.completion = completion;
        }
    }
}
