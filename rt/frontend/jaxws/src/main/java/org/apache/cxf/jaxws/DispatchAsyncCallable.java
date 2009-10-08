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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;

public class DispatchAsyncCallable<T> implements Callable<T> {
    private Dispatch<T> dispatch;
    private T object;
    private AsyncHandler callback;
    private Map<String, Object> ctx;
    
    public DispatchAsyncCallable(Dispatch<T> disp, T obj, AsyncHandler c) {
        dispatch = disp;
        object = obj;
        callback = c;
        ctx = disp.getRequestContext();
    }

    @SuppressWarnings("unchecked")
    public T call() throws Exception {
        dispatch.getRequestContext().clear();
        dispatch.getRequestContext().putAll(ctx);
        final T result = dispatch.invoke(object);
        if (callback != null) {
            callback.handleResponse(new Response<Object>() {

                public Map<String, Object> getContext() {
                    return dispatch.getResponseContext();
                }

                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                public Object get() {
                    return result;
                }

                public boolean isCancelled() {
                    return false;
                }

                public boolean isDone() {
                    return true;
                }

                public Object get(long timeout, TimeUnit unit) {
                    return result;
                }
                
            });
        }
        return result;
    }

}
