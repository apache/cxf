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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ResumeCallback;
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
    
    private CompletionCallback completionCallback;
    
    public AsyncResponseImpl(Message inMessage) {
        inMessage.put(AsyncResponse.class, this);
        inMessage.getExchange().put(ContinuationCallback.class, this);
        this.inMessage = inMessage;
        
        initContinuation();
    }
    
    @Override
    public void resume(Object response) throws IllegalStateException {
        doResume(response);
    }

    @Override
    public void resume(Throwable response) throws IllegalStateException {
        doResume(response);
    }
    
    private synchronized void doResume(Object response) throws IllegalStateException {
        checkCancelled();
        checkSuspended();
        inMessage.getExchange().put(AsyncResponse.class, this);
        cont.setObject(response);
        resumedByApplication = true;
        if (!initialSuspend) {
            cont.resume();
        } else {
            initialSuspend = false;
        }
    }
    
    @Override
    public void cancel() {
        doCancel(null);
    }

    @Override
    public void cancel(int retryAfter) {
        doCancel(Integer.toString(retryAfter));
    }

    @Override
    public void cancel(Date retryAfter) {
        doCancel(HttpUtils.getHttpDateFormat().format(retryAfter));
    }
    
    private synchronized void doCancel(String retryAfterHeader) {
        checkSuspended();
        ResponseBuilder rb = Response.status(503);
        if (retryAfterHeader != null) {
            rb.header(HttpHeaders.RETRY_AFTER, retryAfterHeader);
        }
        doResume(rb.build());
        cancelled = true;
    }

    @Override
    public synchronized boolean isSuspended() {
        return cont.isPending();
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
    public synchronized void setTimeout(long time, TimeUnit unit) throws IllegalStateException {
        checkCancelled();
        checkSuspended();
        inMessage.getExchange().put(AsyncResponse.class, this);
        long timeout = TimeUnit.MILLISECONDS.convert(time, unit);
        initialSuspend = false;
        cont.suspend(timeout);
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    @Override
    public boolean register(Class<?> callback) throws NullPointerException {
        return register(callback, CompletionCallback.class)[0];
    }

    @Override
    public boolean[] register(Class<?> callback, Class<?>... callbacks) throws NullPointerException {
        try {
            return register(callback.newInstance(), CompletionCallback.class);    
        } catch (Throwable t) {
            return new boolean[]{false};
        }
        
    }

    //TODO: API bug, boolean[] needs to be returned...
    @Override
    public boolean register(Object callback) throws NullPointerException {
        return register(callback, CompletionCallback.class, ResumeCallback.class)[0];
    }

    //TODO: API bug, has to be Class<?>...
    @Override
    public boolean[] register(Object callback, Object... callbacks) throws NullPointerException {
        boolean[] result = new boolean[callbacks.length];
        
        for (int i = 0; i < callbacks.length; i++) {
            Object interf = callbacks[i];
            if (interf == null) {
                throw new NullPointerException();
            }
            Class<?> cls = (Class<?>)interf;
            if (cls == CompletionCallback.class && callback instanceof CompletionCallback) {
                completionCallback = (CompletionCallback)callback;
                result[i] = true;
            } else {
                result[i] = false;
            }
        }
        return result;
    }
    
    private void checkCancelled() {
        if (cancelled) {
            throw new IllegalStateException();
        }
    }
    
    private void checkSuspended() {
        if (!initialSuspend && !isSuspended()) {
            throw new IllegalStateException();
        }
    }
    
    @Override
    public void onComplete() {
        done = true;
        if (completionCallback != null) {
            completionCallback.onComplete();
        }
    }

    @Override
    public void onError(Throwable error) {
        if (completionCallback != null) {
            Throwable actualError = error instanceof Fault ? ((Fault)error).getCause() : error;
            completionCallback.onError(actualError);
        }
        
    }
    
    public synchronized void suspendContinuation() {
        initialSuspend = false;
        cont.suspend(AsyncResponse.NO_TIMEOUT);
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
    
    public void reset() {
        cont.reset();
    }    
}
