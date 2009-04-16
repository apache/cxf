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

package org.apache.cxf.jaxws;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.Response;

import org.apache.cxf.endpoint.ClientCallback;

class JaxwsResponseCallback<T> implements Response<T> {
    ClientCallback callback;
    public JaxwsResponseCallback(ClientCallback cb) {
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