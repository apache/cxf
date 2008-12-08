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

package org.apache.cxf.endpoint;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.cxf.message.Message;

/**
 * 
 */
public class ClientCallback implements Future<Object[]> {
    
    protected Map<String, Object> context;
    protected Object[] result;
    protected Throwable exception;
    protected boolean done;
    protected boolean cancelled;
    protected boolean started;
    
    public ClientCallback() {
    }
    
    /**
     * Called when a message is first received prior to any actions 
     * being applied to the message.   The InterceptorChain is setup so 
     * modifications to that can be done.
     */
    public void start(Message msg) {
        started = true;
    }
    
    /**
     * If the processing of the incoming message proceeds normally, this
     * method is called with the response context values and the resulting objects.
     * 
     * The default behavior just stores the objects and calls notifyAll to wake
     * up threads waiting for the response.
     * 
     * @param ctx
     * @param res
     */
    public void handleResponse(Map<String, Object> ctx, Object[] res) {
        context = ctx;
        result = res;
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }
    
    /**
     * If processing of the incoming message results in an exception, this
     * method is called with the resulting exception.
     * 
     * The default behavior just stores the objects and calls notifyAll to wake
     * up threads waiting for the response.
     * 
     * @param ctx
     * @param ex
     */
    public void handleException(Map<String, Object> ctx, Throwable ex) {
        context = ctx;
        exception = ex;
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }
    
    
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!started) {
            cancelled = true;
            synchronized (this) {
                notifyAll();
            }
            return true;
        }
        return false;
    }

    public Map<String, Object> getResponseContext() throws InterruptedException, ExecutionException {
        synchronized (this) {
            if (!done) {
                wait();
            }
        }
        if (cancelled) {
            throw new InterruptedException("Operation Cancelled");
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return context;
    }
    
    public Object[] get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            if (!done) {
                wait();
            }
        }
        if (cancelled) {
            throw new InterruptedException("Operation Cancelled");
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    public Object[] get(long timeout, TimeUnit unit) 
        throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (this) {
            if (!done) {
                unit.timedWait(this, timeout);
            }
        }
        if (cancelled) {
            throw new InterruptedException("Operation Cancelled");
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isDone() {
        return done;
    }
}
