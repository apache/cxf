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
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationCallback;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;


public class AsyncResponseImpl implements AsyncResponse, ContinuationCallback {
    
    private Continuation cont;
    private Message inMessage;
    private boolean initialSuspend;
    private boolean cancelled;
    private volatile boolean done;
    private boolean resumedByApplication;
    private TimeoutHandler timeoutHandler;
    
    private List<CompletionCallback> completionCallbacks = new LinkedList<CompletionCallback>();
    private List<ConnectionCallback> connectionCallbacks = new LinkedList<ConnectionCallback>();
    private Throwable unmappedThrowable;
    
    public AsyncResponseImpl(Message inMessage) {
        inMessage.put(AsyncResponse.class, this);
        inMessage.getExchange().put(ContinuationCallback.class, this);
        this.inMessage = inMessage;
        
        initContinuation();
    }
    
    @Override
    public boolean resume(Object response) {
        return doResume(response);
    }

    @Override
    public boolean resume(Throwable response) {
        return doResume(response);
    }
    
    private boolean isCancelledOrNotSuspended() {
        return isCancelled() || !isSuspended();
    }
    
    private synchronized boolean doResume(Object response) {
        if (isCancelledOrNotSuspended()) {
            return false;
        }
        return doResumeFinal(response);
    }
    private synchronized boolean doResumeFinal(Object response) {
        inMessage.getExchange().put(AsyncResponse.class, this);
        cont.setObject(response);
        resumedByApplication = true;
        if (!initialSuspend) {
            cont.resume();
        } else {
            initialSuspend = false;
        }
        return true;
    }
    
    @Override
    public boolean cancel() {
        return doCancel(null);
    }

    @Override
    public boolean cancel(int retryAfter) {
        return doCancel(Integer.toString(retryAfter));
    }

    @Override
    public boolean cancel(Date retryAfter) {
        return doCancel(HttpUtils.getHttpDateFormat().format(retryAfter));
    }
    
    private synchronized boolean doCancel(String retryAfterHeader) {
        if (cancelled) {
            return true;
        }
        if (!isSuspended()) {
            return false;
        }
        ResponseBuilder rb = Response.status(503);
        if (retryAfterHeader != null) {
            rb.header(HttpHeaders.RETRY_AFTER, retryAfterHeader);
        }
        cancelled = true;
        doResumeFinal(rb.build());
        return cancelled;
    }

    @Override
    public synchronized boolean isSuspended() {
        return initialSuspend || cont.isPending();
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized boolean setTimeout(long time, TimeUnit unit) throws IllegalStateException {
        if (isCancelledOrNotSuspended()) {
            return false;
        }
        inMessage.getExchange().put(AsyncResponse.class, this);
        long timeout = TimeUnit.MILLISECONDS.convert(time, unit);
        initialSuspend = false;
        cont.suspend(timeout);
        return true;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    @Override
    public Collection<Class<?>> register(Class<?> callback) throws NullPointerException {
        return register(callback, new Class<?>[]{}).get(callback);
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) 
        throws NullPointerException {
        try {
            Object[] extraCallbacks = new Object[callbacks.length];
            for (int i = 0; i < callbacks.length; i++) {
                extraCallbacks[i] = callbacks[i].newInstance();
            }
            return register(callback.newInstance(), extraCallbacks);    
        } catch (Throwable t) {
            return Collections.emptyMap();
        }
        
    }

    @Override
    public Collection<Class<?>> register(Object callback) throws NullPointerException {
        return register(callback, new Object[]{}).get(callback.getClass());
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) 
        throws NullPointerException {
        Map<Class<?>, Collection<Class<?>>> map = 
            new HashMap<Class<?>, Collection<Class<?>>>();
    
        Object[] allCallbacks = new Object[1 + callbacks.length];
        allCallbacks[0] = callback;
        System.arraycopy(callbacks, 0, allCallbacks, 1, callbacks.length);
        
        for (int i = 0; i < allCallbacks.length; i++) {
            if (allCallbacks[i] == null) {
                throw new NullPointerException();
            }
            Class<?> callbackCls = allCallbacks[i].getClass();
            Collection<Class<?>> knownCallbacks = map.get(callbackCls);
            if (knownCallbacks == null) {
                knownCallbacks = new HashSet<Class<?>>();
                map.put(callbackCls, knownCallbacks);
            }
            
            if (allCallbacks[i] instanceof CompletionCallback) {
                knownCallbacks.add(CompletionCallback.class);
                completionCallbacks.add((CompletionCallback)allCallbacks[i]);        
            } else if (allCallbacks[i] instanceof ConnectionCallback) {
                knownCallbacks.add(ConnectionCallback.class);
                connectionCallbacks.add((ConnectionCallback)allCallbacks[i]);        
            }
        }
        return map;
    }
    
    @Override
    public void onComplete() {
        done = true;
        updateCompletionCallbacks(unmappedThrowable);
    }

    @Override
    public void onError(Throwable error) {
        updateCompletionCallbacks(error);
    }
    
    private void updateCompletionCallbacks(Throwable error) {
        Throwable actualError = error instanceof Fault ? ((Fault)error).getCause() : error;
        for (CompletionCallback completionCallback : completionCallbacks) {
            completionCallback.onComplete(actualError);
        }
    }
    
    @Override
    public void onDisconnect() {
        for (ConnectionCallback connectionCallback : connectionCallbacks) {
            connectionCallback.onDisconnect(this);
        }
    }
    
    public synchronized boolean suspendContinuationIfNeeded() {
        if (!cont.isPending() && !resumedByApplication) {
            initialSuspend = false;
            cont.suspend(AsyncResponse.NO_TIMEOUT);
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized Object getResponseObject() {
        Object obj = cont.getObject();
        if (!(obj instanceof Response) && !(obj instanceof Throwable)) {
            obj = Response.ok().entity(obj).build();    
        }
        return obj;
    }
    
    public synchronized boolean isResumedByApplication() {
        return resumedByApplication;
    }
    
    public synchronized void handleTimeout() {
        if (!resumedByApplication) {
            if (timeoutHandler != null) {
                timeoutHandler.handleTimeout(this);
            } else {
                cont.setObject(new ServiceUnavailableException());
            }
        }
    }

    private void initContinuation() {
        ContinuationProvider provider = 
            (ContinuationProvider)inMessage.get(ContinuationProvider.class.getName());
        cont = provider.getContinuation();
        initialSuspend = true;
    }
    
    public void prepareContinuation() {
        initContinuation();
    }
    
    public void setUnmappedThrowable(Throwable t) {
        unmappedThrowable = t;
    }
    public void reset() {
        cont.reset();
    }
    
}
