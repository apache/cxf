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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.client.InvocationCallback;

import org.apache.cxf.endpoint.ClientCallback;

class JaxrsClientCallback<T> extends ClientCallback {
    private final InvocationCallback<T> handler;
    private final Type outType;
    private final Class<?> responseClass;
    
    JaxrsClientCallback(final InvocationCallback<T> handler, 
                        Class<?> responseClass, 
                        Type outGenericType) {
        this.handler = handler;
        this.outType = outGenericType;
        this.responseClass = responseClass;
    }
    
    public InvocationCallback<T> getHandler() {
        return handler;
    }
    
    public Type getOutGenericType() {
        return outType;
    }
    public Class<?> getResponseClass() {
        return responseClass;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = super.cancel(mayInterruptIfRunning);
        if (result && handler != null) {
            handler.failed(new CancellationException());
        }
        return result;
    }
    
    public Future<T> createFuture() {
        return new JaxrsResponseFuture<T>(this);
    }
    
    public CompletionStage<T> createCompletionStage() {
        return null;
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
        exception = ex;
        if (handler != null) {
            handler.failed(exception);
        }
        done = true;
        synchronized (this) {
            notifyAll();
        }
    }
    
    
    
    static class JaxrsResponseFuture<T> implements Future<T> {
        JaxrsClientCallback<T> callback;
        JaxrsResponseFuture(JaxrsClientCallback<T> cb) {
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
                if (callback.handler != null) {
                    callback.handler.failed((InterruptedException)ex);
                }
                throw ex;
            }
        }
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
            try {
                return getObject(callback.get(timeout, unit)[0]);
            } catch (InterruptedException ex) {
                if (callback.handler != null) {
                    callback.handler.failed((InterruptedException)ex);
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
    static class JaxrsResponseStage<T> implements CompletionStage<T> {
        JaxrsClientCallback<T> callback;
        JaxrsResponseStage(JaxrsClientCallback<T> cb) {
            callback = cb;
        }
        @Override
        public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenRun(Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenRunAsync(Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                                     BiFunction<? super T, ? super U, ? extends V> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                          BiFunction<? super T, ? super U, ? extends V> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                          BiFunction<? super T, ? super U, ? extends V> fn,
                                                          Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                        BiConsumer<? super T, ? super U> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                             BiConsumer<? super T, ? super U> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                             BiConsumer<? super T, ? super U> action,
                                                             Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
                                                       Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
                                                    Function<? super T, U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                         Function<? super T, U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                         Function<? super T, U> fn, Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
                                                  Consumer<? super T> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                       Consumer<? super T> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                       Consumer<? super T> action, Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
                                                         Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
                                                       Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
                                                    Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                                  Executor executor) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public CompletableFuture<T> toCompletableFuture() {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}