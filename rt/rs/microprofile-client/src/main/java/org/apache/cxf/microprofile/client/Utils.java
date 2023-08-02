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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.cxf.jaxrs.client.AbstractClient;
import org.apache.cxf.jaxrs.ext.MessageContext;

public final class Utils {

    private Utils() {
    }

    public static ExecutorService getExecutorService(MessageContext mc) {
        ExecutorService es = (ExecutorService) mc.get(AbstractClient.EXECUTOR_SERVICE_PROPERTY);
        if (es == null) {
            es = getCommonPool();
        }
        return es;
    }
    
    public static ExecutorService defaultExecutorService() {
        return new LazyForkJoinExecutor();
    }
    
    private static final class LazyForkJoinExecutor implements ExecutorService {
        @Override
        public void execute(Runnable command) {
            getCommonPool().execute(command);
        }

        @Override
        public void shutdown() {
            getCommonPool().shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return getCommonPool().shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return getCommonPool().isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return getCommonPool().isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return getCommonPool().awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return getCommonPool().submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return getCommonPool().submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return getCommonPool().submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return getCommonPool().invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                TimeUnit unit) throws InterruptedException {
            return getCommonPool().invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) 
                throws InterruptedException, ExecutionException {
            return getCommonPool().invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, 
                TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return getCommonPool().invokeAny(tasks, timeout, unit);
        }
    }
    
    private static ExecutorService getCommonPool() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<ExecutorService>) () -> {
                return ForkJoinPool.commonPool();
            });
        } else {
            return ForkJoinPool.commonPool();
        }
    }
}