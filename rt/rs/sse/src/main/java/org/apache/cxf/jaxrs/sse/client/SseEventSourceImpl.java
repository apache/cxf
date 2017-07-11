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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
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
    private final Collection<InboundSseEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<SseSourceState> state = new AtomicReference<>(SseSourceState.CLOSED);
    
    // It may happen that open() and close() could be called on separate threads
    private volatile ScheduledExecutorService executor;
    private volatile InboundSseEventProcessor processor; 
    private volatile TimeUnit unit;
    private volatile long delay;

    private class InboundSseEventListenerDelegate implements InboundSseEventListener {
        private String lastEventId;
        
        @Override
        public void onNext(InboundSseEvent event) {
            lastEventId = event.getId();
            listeners.forEach(listener -> listener.onNext(event));
            
            // Reconnect delay is set in milliseconds
            if (event.isReconnectDelaySet()) {
                unit = TimeUnit.MILLISECONDS;
                delay = event.getReconnectDelay();
            }
        }

        @Override
        public void onError(Throwable ex) {
            listeners.forEach(listener -> listener.onError(ex));
            if (delay >= 0 && unit != null) {
                scheduleReconnect(delay, unit, lastEventId);
            }
        }

        @Override
        public void onComplete() {
            listeners.forEach(InboundSseEventListener::onComplete);
            if (delay >= 0 && unit != null) {
                scheduleReconnect(delay, unit, lastEventId);
            }
        }
    }
    
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
    
    /**
     * https://www.w3.org/TR/2012/WD-eventsource-20120426/#dom-eventsource-connecting
     */
    private enum SseSourceState {
        CONNECTING,
        OPEN,
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
        if (!state.compareAndSet(SseSourceState.CLOSED, SseSourceState.CONNECTING)) {
            throw new IllegalStateException("The SseEventSource is already in " + state.get() + " state");
        }

        // Create the executor for scheduling the reconnect tasks 
        executor = 
            (ScheduledExecutorService)target.getConfiguration().getProperty("scheduledExecutorService");
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();    
        }
        
        
        final Object lastEventId = target.getConfiguration().getProperty(HttpHeaders.LAST_EVENT_ID_HEADER);
        connect(lastEventId != null ? lastEventId.toString() : null);
    }

    private void connect(String lastEventId) {
        final InboundSseEventListenerDelegate delegate = new InboundSseEventListenerDelegate();
        Response response = null;
        
        try {
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            if (lastEventId != null) {
                headers.putSingle(HttpHeaders.LAST_EVENT_ID_HEADER, lastEventId);
            }
            
            response = target
                .request(MediaType.SERVER_SENT_EVENTS)
                .headers(headers)
                .get();
            
            // A client can be told to stop reconnecting using the HTTP 204 No Content 
            // response code. In this case, we should stop here.
            if (response.getStatus() == 204) {
                LOG.fine("SSE endpoint " + target.getUri() + " returns no data, disconnecting");
                state.compareAndSet(SseSourceState.CONNECTING, SseSourceState.CLOSED);
                response.close();
                return;
            }

            final Endpoint endpoint = WebClient.getConfig(target).getEndpoint();
            processor = new InboundSseEventProcessor(endpoint, delegate);
            processor.run(response);
            
            state.compareAndSet(SseSourceState.CONNECTING, SseSourceState.OPEN);
            LOG.fine("Opened SSE connection to " + target.getUri());
        } catch (final Exception ex) {
            if (processor != null) {
                processor.close(1, TimeUnit.SECONDS);
                processor = null;
            }
            
            if (response != null) {
                response.close();
            }

            // We don't change the state here as the reconnection will be scheduled (if configured)
            LOG.fine("Failed to open SSE connection to " + target.getUri() + ". " + ex.getMessage());
            delegate.onError(ex);
        }
    }

    @Override
    public boolean isOpen() {
        return state.get() == SseSourceState.OPEN;
    }

    @Override
    public boolean close(long timeout, TimeUnit unit) {
        if (state.get() == SseSourceState.CLOSED) {
            return true;
        }
        
        if (state.compareAndSet(SseSourceState.CONNECTING, SseSourceState.CLOSED)) {
            LOG.fine("The SseEventSource was not connected, closing anyway");
        } else if (!state.compareAndSet(SseSourceState.OPEN, SseSourceState.CLOSED)) {
            throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get() + " state");
        }
        
        if (executor != null) {
            executor.shutdown();
        }

        // Should never happen
        if (processor == null) {
            return true;
        }
        
        return processor.close(timeout, unit); 
    }
    
    private void scheduleReconnect(long delay, TimeUnit unit, String lastEventId) {
        // If delay == RECONNECT_NOT_SET, no reconnection attempt should be performed
        if (delay < 0 || executor == null) {
            return;
        }
        
        // If the event source is already closed, do nothing
        if (state.get() == SseSourceState.CLOSED) {
            return;
        }
        
        // If the connection was still on connecting state, just try to reconnect
        if (state.get() != SseSourceState.CONNECTING && !state.compareAndSet(SseSourceState.OPEN, SseSourceState.CONNECTING)) {
            throw new IllegalStateException("The SseEventSource is not opened, but in " + state.get() + 
                " state, unable to reconnect");
        }
                
        executor.schedule(() -> {
            // If we are still in connecting state (not closed/open), let's try to reconnect
            if (state.get() == SseSourceState.CONNECTING) {
                LOG.fine("Reestablishing SSE connection to " + target.getUri());
                connect(lastEventId);
            }
        }, delay, unit);
        
        LOG.fine("The reconnection attempt to " + target.getUri() + " is scheduled in " + 
            unit.toMillis(delay) + "ms");
    }
}
