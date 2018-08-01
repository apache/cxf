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

package org.apache.cxf.microprofile.client;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.InvocationCallback;

import org.apache.cxf.jaxrs.client.JaxrsClientCallback;

public class MPRestClientCallback<T> extends JaxrsClientCallback {

    public MPRestClientCallback(InvocationCallback<T> handler,
                                Class<?> responseClass,
                                Type outGenericType) {
        super(handler, responseClass, outGenericType);
    }

    @Override
    public Future<T> createFuture() {
        return new MPRestClientResponseFuture<T>(this);
    }

    static class MPRestClientResponseFuture<T> extends CompletableFuture<T> implements Future<T> {
        MPRestClientCallback<T> callback;
        MPRestClientResponseFuture(MPRestClientCallback<T> cb) {
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

        public T get() throws InterruptedException, ExecutionException {
            try {
                return getObject(callback.get()[0]);
            } catch (InterruptedException ex) {
                InvocationCallback<T> handler = callback.getHandler();
                if (handler != null) {
                    handler.failed(ex);
                }
                throw ex;
            }
        }
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
            try {
                return getObject(callback.get(timeout, unit)[0]);
            } catch (InterruptedException ex) {
                InvocationCallback<T> handler = callback.getHandler();
                if (handler != null) {
                    handler.failed(ex);
                }
                throw ex;
            }
        }

        @SuppressWarnings("unchecked")
        private T getObject(Object object) {
            return (T)object;
        }

        public boolean isCancelled() {
            return callback.isCancelled();
        }
        public boolean isDone() {
            return callback.isDone();
        }
    }
}