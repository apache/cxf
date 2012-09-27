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

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;


public class AsyncResponseImpl implements AsyncResponse {
    
    private Continuation cont;
    private Object responseObject;
    private long timeout = 5000;
    private Message inMessage;
    private boolean suspended;
    private boolean cancelled;
    public AsyncResponseImpl(Message inMessage) {
        ContinuationProvider provider = 
            (ContinuationProvider)inMessage.get(ContinuationProvider.class.getName());
        cont = provider.getContinuation();
        inMessage.put(AsyncResponse.class, this);
        this.inMessage = inMessage;
       
    }
    
    @Override
    public void resume(Object response) throws IllegalStateException {
        doResume(response);
    }

    @Override
    public void resume(Throwable response) throws IllegalStateException {
        doResume(response);
    }
    
    private void doResume(Object response) throws IllegalStateException {
        responseObject = response;
        inMessage.getExchange().put(AsyncResponse.class, this);
        suspended = false;
        cont.resume();
    }
    
    @Override
    public void cancel() {
        cancel(-1);
    }

    @Override
    //TODO: has to be long
    public void cancel(int retryAfter) {
        cancelled = true;
        doResume(Response.status(503).header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfter)).build());
    }

    @Override
    public void cancel(Date retryAfter) {
        cancel((int)(retryAfter.getTime() - new Date().getTime()));
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setTimeout(long time, TimeUnit unit) throws IllegalStateException {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean register(Class<?> callback) throws NullPointerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean[] register(Class<?> callback, Class<?>... callbacks) throws NullPointerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean register(Object callback) throws NullPointerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean[] register(Object callback, Object... callbacks) throws NullPointerException {
        // TODO Auto-generated method stub
        return null;
    }
    
    // these methods are called by the runtime, not part of AsyncResponse    
    public void suspend() {
        cont.setObject(this);
        cont.suspend(timeout);
    }
    
    public Object getResponseObject() {
        return responseObject;
    }
}
