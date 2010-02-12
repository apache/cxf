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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

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
    private long secondsToKeepState;
    
    public ThreadLocalClientState(String baseURI) {
        this.initialState = new LocalClientState(URI.create(baseURI));
    }
    
    public ThreadLocalClientState(LocalClientState initialState) {
        this.initialState = initialState;
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
    
    public void setResponseBuilder(ResponseBuilder responseBuilder) {
        getState().setResponseBuilder(responseBuilder);
    }
    
    public ResponseBuilder getResponseBuilder() {
        return getState().getResponseBuilder();
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
    
    public ClientState newState(URI baseURI, 
                                MultivaluedMap<String, String> headers,
                                MultivaluedMap<String, String> templates) {
        LocalClientState ls = new LocalClientState(baseURI);
        ls.setRequestHeaders(headers);
        ls.setTemplates(templates);
        return new ThreadLocalClientState(ls);
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
            if (secondsToKeepState > 0) {
                long currentTime = System.currentTimeMillis();
                checkpointMap.put(Thread.currentThread(), currentTime);
                new CleanupThread(Thread.currentThread(), currentTime).start();
            }
        }
        return cs;
    }

    public void setSecondsToKeepState(long secondsToKeepState) {
        this.secondsToKeepState = secondsToKeepState;
        if (secondsToKeepState > 0) {
            checkpointMap = new ConcurrentHashMap<Thread, Long>();
        }
    }

    private class CleanupThread extends Thread {
        private Thread thread;
        private long originalTime;
        
        public CleanupThread(Thread thread, long originalTime) {
            this.thread = thread;
            this.originalTime = originalTime;
        }
        
        @Override
        public void run() {
            try {
                Thread.sleep(secondsToKeepState);
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
