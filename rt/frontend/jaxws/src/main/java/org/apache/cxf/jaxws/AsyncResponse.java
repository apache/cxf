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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.Response;


public class AsyncResponse<T> implements Response<T> {

    private final Future<T> obj;
    private T result;
    private Class<T> cls;
    
    public AsyncResponse(Future<T> object, Class<T> c) {
        obj = object;
        cls = c;
    }
    
    public boolean cancel(boolean interrupt) {
        return obj.cancel(interrupt);     
    }
    
    public boolean isCancelled() {
        return obj.isCancelled(); 
    }

    public boolean isDone() {
        return obj.isDone();
    }

    public synchronized T get() throws InterruptedException, ExecutionException {
        if (result == null) {
            result = cls.cast(obj.get());
        } 
        return result;
    }

    
    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (result == null) {
            result = cls.cast(obj.get(timeout, unit));
        } 
        return result;
    }

    public Map<String, Object> getContext() {
        return null;
    }
    
    
}
