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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class JaxrsClientStageCallback<T> extends JaxrsClientCallback<T>  {
    private CompletableFuture<T> cf;
    
    public JaxrsClientStageCallback(Class<?> responseClass, 
                             Type outGenericType,
                             Executor ex) {
        super(null, responseClass, outGenericType);
        
        Supplier<T> supplier = new SupplierImpl();
        cf = ex == null ? CompletableFuture.supplyAsync(supplier) 
            : CompletableFuture.supplyAsync(supplier, ex);
    }
    
    public CompletionStage<T> getCompletionStage() {
        return cf;
    }
    
    @Override
    public void handleResponse(Map<String, Object> ctx, Object[] res) {
        context = ctx;
        result = res;
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void handleException(Map<String, Object> ctx, final Throwable ex) {
        context = ctx;
        exception = ex;
        cf.completeExceptionally(ex);
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = super.cancel(mayInterruptIfRunning);
        if (result) {
            cf.cancel(mayInterruptIfRunning);
        }
        return result;
    }

    private class SupplierImpl implements Supplier<T> {

        @SuppressWarnings("unchecked")
        @Override
        public T get() {
            try {
                return (T)JaxrsClientStageCallback.this.get()[0];
            } catch (Exception ex) {
                cf.completeExceptionally(ex);
                return null;
            }
        }
        
    }
}