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
package org.apache.cxf.jaxrs.client;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Keeps the client state such as the baseURI, currentURI, requestHeaders, current response
 * in a thread local storage
 *
 */
public class ThreadLocalClientState implements ClientState {
    private Map<Thread, LocalClientState> state =
        Collections.synchronizedMap(new WeakHashMap<Thread, LocalClientState>());

    private LocalClientState initialState;

    private Map<Thread, Long> checkpointMap;
    private long timeToKeepState;

    public ThreadLocalClientState(String baseURI) {
        this(baseURI, Collections.emptyMap());
    }
    
    public ThreadLocalClientState(String baseURI, Map<String, Object> properties) {
        this.initialState = new LocalClientState(URI.create(baseURI), properties);
    }

    public ThreadLocalClientState(String baseURI, long timeToKeepState) {
        this(baseURI, timeToKeepState, Collections.emptyMap());
    }

    public ThreadLocalClientState(String baseURI, long timeToKeepState, Map<String, Object> properties) {
        this.initialState = new LocalClientState(URI.create(baseURI), properties);
        this.timeToKeepState = timeToKeepState;
    }

    public ThreadLocalClientState(LocalClientState initialState, long timeToKeepState) {
        this.initialState = initialState;
        this.timeToKeepState = timeToKeepState;
    }

    public void setCurrentBuilder(UriBuilder currentBuilder) {
        getState().setCurrentBuilder(currentBuilder);
    }

    public UriBuilder getCurrentBuilder() {
        return getState().getCurrentBuilder();
    }

    public void setBaseURI(URI baseURI) {
        getState().setBaseURI(baseURI);
    }

    public URI getBaseURI() {
        return getState().getBaseURI();
    }

    public void setResponse(Response response) {
        getState().setResponse(response);
    }

    public Response getResponse() {
        return getState().getResponse();
    }

    public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
        getState().setRequestHeaders(requestHeaders);
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        return getState().getRequestHeaders();
    }

    public MultivaluedMap<String, String> getTemplates() {
        return getState().getTemplates();
    }

    public void setTemplates(MultivaluedMap<String, String> map) {
        getState().setTemplates(map);
    }

    public void reset() {
        removeThreadLocalState(Thread.currentThread());
    }

    public ClientState newState(URI currentURI,
                                MultivaluedMap<String, String> headers,
                                MultivaluedMap<String, String> templates) {
        LocalClientState ls = (LocalClientState)getState().newState(currentURI, headers, templates);
        return new ThreadLocalClientState(ls, timeToKeepState);
    }
    
    @Override
    public ClientState newState(URI currentURI,
            MultivaluedMap<String, String> headers,
            MultivaluedMap<String, String> templates,
            Map<String, Object> properties) {
        LocalClientState ls = (LocalClientState)getState().newState(currentURI, headers, templates, properties);
        return new ThreadLocalClientState(ls, timeToKeepState);
    }

    private void removeThreadLocalState(Thread t) {
        state.remove(t);
        if (checkpointMap != null) {
            checkpointMap.remove(t);
        }
    }

    protected ClientState getState() {
        LocalClientState cs = state.get(Thread.currentThread());
        if (cs == null) {
            cs = new LocalClientState(initialState);
            state.put(Thread.currentThread(), cs);
            if (timeToKeepState > 0) {
                prepareCheckpointMap();
                long currentTime = System.currentTimeMillis();
                checkpointMap.put(Thread.currentThread(), currentTime);
                Thread clThread = new CleanupThread(Thread.currentThread(), currentTime);
                clThread.setName("Client state cleanup thread " + clThread.hashCode());
                clThread.start();
            }
        }
        return cs;
    }

    public void setTimeToKeepState(long timeToKeepState) {
        this.timeToKeepState = timeToKeepState;
        if (timeToKeepState > 0) {
            prepareCheckpointMap();
        }
    }

    private void prepareCheckpointMap() {
        if (checkpointMap == null) {
            checkpointMap = new ConcurrentHashMap<>();
        }
    }

    private class CleanupThread extends Thread {
        private Thread thread;
        private long originalTime;

        CleanupThread(Thread thread, long originalTime) {
            this.thread = thread;
            this.originalTime = originalTime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(timeToKeepState);
                long actualTime = checkpointMap.get(thread);
                // if times do not match then the original worker thread
                // has called reset() but came back again to create new local state
                // hence there's another cleanup thread nearby which will clean the state
                if (actualTime == originalTime) {
                    removeThreadLocalState(thread);
                }
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }


}
