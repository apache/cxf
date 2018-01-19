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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 
 */
public class CxfCompletableFuture<T> extends CompletableFuture<T> {

    private final ExecutorService executor;

    public CxfCompletableFuture(ExecutorService es) {
        super();
        executor = es;
    }

    public static <U> CxfCompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                          ExecutorService executor) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        CxfCompletableFuture<U> d = new CxfCompletableFuture<U>(executor);
        executor.execute(new Runnable() {

            @Override
            public void run() {
                if (d != null && supplier != null) {
                    try {
                        d.complete(supplier.get());
                    } catch (Throwable t) {
                        d.completeExceptionally(t);
                    }
                }
            } });
        return d;
    }



    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, executor);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other,
                                                     Runnable action) {
        return runAfterBothAsync(other, action, executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                       Runnable action) {
        return runAfterEitherAsync(other, action, executor);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                           BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, 
                                                        BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, executor);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, executor);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, executor);
    }
}
