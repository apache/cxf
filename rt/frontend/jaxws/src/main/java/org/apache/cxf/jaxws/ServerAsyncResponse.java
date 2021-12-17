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

/**
 *
 */
public class ServerAsyncResponse<T> implements jakarta.xml.ws.Response<T> {
    T value;
    boolean done;
    Throwable throwable;

    /**
     * Currently unused
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
    /**
     * Currently unused
     */
    public boolean isCancelled() {
        return false;
    }
    public boolean isDone() {
        return done;
    }
    public void set(T t) {
        value = t;
        done = true;
    }
    public T get() throws InterruptedException, ExecutionException {
        if (throwable != null) {
            throw new ExecutionException(throwable);
        }
        return value;
    }
    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }
    public void exception(Throwable ex) {
        throwable = ex;
        done = true;
    }
    /**
     * Currently unused
     */
    public Map<String, Object> getContext() {
        return null;
    }

}
