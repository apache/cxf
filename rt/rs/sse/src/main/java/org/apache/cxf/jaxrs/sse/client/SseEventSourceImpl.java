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
package org.apache.cxf.jaxrs.sse.client;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.WebClient;

/**
 * SSE Event Source implementation 
 */
public class SseEventSourceImpl implements SseEventSource {
    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSourceImpl.class);
    
    private final WebTarget target;
    private final long delay;
    private final TimeUnit unit;
    private final Collection<InboundSseEventListener> listeners = new CopyOnWriteArrayList<>();
    
    // It may happen that open() and close() could be called on separate threads
    private volatile InboundSseEventProcessor processor; 
    
    private class InboundSseEventListenerImpl implements InboundSseEventListener {
        private final Consumer<InboundSseEvent> onEvent;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e) {
            this(e, ex -> {}, () -> {});
        }
        
        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t) {
            this(e, t, () -> {});
        }

        InboundSseEventListenerImpl(Consumer<InboundSseEvent> e, Consumer<Throwable> t, Runnable c) {
            this.onEvent = e;
            this.onError = t;
            this.onComplete = c;
        }

        @Override
        public void onNext(InboundSseEvent event) {
            onEvent.accept(event);
        }

        @Override
        public void onError(Throwable ex) {
            onError.accept(ex);
        }

        @Override
        public void onComplete() {
            onComplete.run();
        }
    }

    private final AtomicReference<SseSourceState> state = 
        new AtomicReference<>(SseSourceState.CLOSED);
    
    private enum SseSourceState {
        OPENING,
        OPENED,
        CLOSED
    }
    
    SseEventSourceImpl(WebTarget target, long delay, TimeUnit unit) {
        this.target = target;
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent) {
        listeners.add(new InboundSseEventListenerImpl(onEvent));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError));
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        listeners.add(new InboundSseEventListenerImpl(onEvent, onError, onComplete));
    }

    @Override
    public void open() {
        if (!state.compareAndSet(SseSourceState.CLOSED, SseSourceState.OPENING)) {
            throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
        }

        Response response = null; 
        try {
            response = target
                .request(MediaType.SERVER_SENT_EVENTS)
                .get();

            final Endpoint endpoint = WebClient.getConfig(target).getEndpoint();
            processor = new InboundSseEventProcessor(endpoint,
                new InboundSseEventListener() {
                    @Override
                    public void onNext(InboundSseEvent event) {
                        listeners.forEach(listener -> listener.onNext(event));
                    }
        
                    @Override
                    public void onError(Throwable ex) {
                        listeners.forEach(listener -> listener.onError(ex));
                        if (delay > 0 && unit != null) {
                            // TODO: Schedule reconnect here
                        }
                    }
        
                    @Override
                    public void onComplete() {
                        listeners.forEach(InboundSseEventListener::onComplete);
                    }
                }
            );

            processor.run(response);
            state.compareAndSet(SseSourceState.OPENING, SseSourceState.OPENED);
            
            LOG.fine("Opened SSE connection to " + target.getUri());
        } catch (final Exception ex) {
            state.compareAndSet(SseSourceState.OPENING, SseSourceState.CLOSED);
            LOG.fine("Failed to open SSE connection to " + target.getUri() + ". " + ex.getMessage());
            
            if (response != null) {
                response.close();
            }
            
            listeners.forEach(listener -> listener.onError(ex));
        }
    }

    @Override
    public boolean isOpen() {
        return state.get() == SseSourceState.OPENED;
    }

    @Override
    public boolean close(long timeout, TimeUnit unit) {
        if (state.get() == SseSourceState.CLOSED) {
            return true;
        }
        
        if (!state.compareAndSet(SseSourceState.OPENED, SseSourceState.CLOSED)) {
            throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get() + " state");
        }

        // Should never happen
        if (processor == null) {
            return true;
        }
        
        return processor.close(timeout, unit); 
    }
}
