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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.InvocationCallback;

import org.apache.cxf.endpoint.ClientCallback;

class JaxrsClientCallback<T> extends ClientCallback {
    private final InvocationCallback<T> handler;
    private final Type outType;
    private final Class<?> responseClass;
    
    public JaxrsClientCallback(final InvocationCallback<T> handler, 
                               Class<?> responseClass, 
                               Type outGenericType) {
        this.handler = handler;
        this.outType = outGenericType;
        this.responseClass = responseClass;
    }
    
    public Type getOutGenericType() {
        return outType;
    }
    public Class<?> getResponseClass() {
        return responseClass;
    }
    
    public Future<T> createFuture() {
        return new JaxrsResponseCallback<T>(this);
    }
    static class JaxrsResponseCallback<T> implements Future<T> {
        JaxrsClientCallback<T> callback;
        public JaxrsResponseCallback(JaxrsClientCallback<T> cb) {
            callback = cb;
        }
        
        public Map<String, Object> getContext() {
            try {
                return callback.getResponseContext();
            } catch (Exception ex) {
                return null;
            }
        }
        public boolean cancel(boolean mayInterruptIfRunning) {
            return callback.cancel(mayInterruptIfRunning);
        }
        @SuppressWarnings("unchecked")
        public T get() throws InterruptedException, ExecutionException {
            return (T)callback.get()[0];
        }
        @SuppressWarnings("unchecked")
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
            return (T)callback.get(timeout, unit)[0];
        }
        public boolean isCancelled() {
            return callback.isCancelled();
        }
        public boolean isDone() {
            return callback.isDone();
        }
    }
    
    
    @SuppressWarnings("unchecked")
    public void handleResponse(Map<String, Object> ctx, Object[] res) {
        context = ctx;
        result = res;
        if (handler != null) {
            handler.completed((T)res[0]);
        }
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void handleException(Map<String, Object> ctx, final Throwable ex) {
        context = ctx;
        if (ex instanceof ProcessingException) {
            exception = ex;
        } else {
            exception = new ProcessingException(ex);
        }
        if (handler != null) {
            handler.failed((ProcessingException)exception);
        }
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }
}