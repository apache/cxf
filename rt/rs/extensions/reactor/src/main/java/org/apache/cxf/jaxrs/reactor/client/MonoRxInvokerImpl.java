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
package org.apache.cxf.jaxrs.reactor.client;

import org.apache.cxf.jaxrs.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.apache.cxf.jaxrs.reactor.client.ReactorUtils.TRACE;
import static org.apache.cxf.jaxrs.reactor.client.ReactorUtils.toCompletableFuture;

public class MonoRxInvokerImpl implements MonoRxInvoker {
    private final WebClient webClient;
    private final Scheduler scheduler;

    MonoRxInvokerImpl(WebClient webClient, ExecutorService executorService) {
        this.webClient = webClient;
        this.scheduler = executorService == null ? null : Schedulers.fromExecutorService(executorService);
    }

    @Override
    public Mono get() {
        return method(HttpMethod.GET);
    }

    @Override
    public <R> Mono<R> get(Class<R> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <R> Mono<R> get(GenericType<R> genericType) {
        return method(HttpMethod.GET, genericType);
    }

    @Override
    public Mono<Response> put(Entity<?> entity) {
        return method(HttpMethod.PUT, entity);
    }

    @Override
    public <R> Mono<R> put(Entity<?> entity, Class<R> responseType) {
        return method(HttpMethod.PUT, responseType);
    }

    @Override
    public <R> Mono<R> put(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.PUT, entity, genericType);
    }

    @Override
    public Mono<Response> post(Entity<?> entity) {
        return method(HttpMethod.POST, entity);
    }

    @Override
    public <R> Mono<R> post(Entity<?> entity, Class<R> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <R> Mono<R> post(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.POST, entity, genericType);
    }

    @Override
    public Mono<Response> delete() {
        return method(HttpMethod.DELETE);
    }

    @Override
    public <R> Mono<R> delete(Class<R> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <R> Mono<R> delete(GenericType<R> genericType) {
        return method(HttpMethod.DELETE, genericType);
    }

    @Override
    public Mono<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public Mono<Response> options() {
        return method(HttpMethod.OPTIONS);
    }

    @Override
    public <R> Mono<R> options(Class<R> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <R> Mono<R> options(GenericType<R> genericType) {
        return method(HttpMethod.OPTIONS, genericType);
    }

    @Override
    public Mono<Response> trace() {
        return method(TRACE);
    }

    @Override
    public <R> Mono<R> trace(Class<R> responseType) {
        return method(TRACE, responseType);
    }

    @Override
    public <R> Mono<R> trace(GenericType<R> genericType) {
        return method(TRACE, genericType);
    }

    @Override
    public Mono<Response> method(String name) {
        return method(name, Response.class);
    }

    @Override
    public <R> Mono<R> method(String name, Class<R> responseType) {
        return mono(webClient.async().method(name, responseType));
    }

    @Override
    public <R> Mono<R> method(String name, GenericType<R> genericType) {
        return mono(webClient.async().method(name, genericType));
    }

    @Override
    public Mono<Response> method(String name, Entity<?> entity) {
        return method(name, entity, Response.class);
    }

    @Override
    public <R> Mono<R> method(String name, Entity<?> entity, Class<R> responseType) {
        return mono(webClient.async().method(name, entity, responseType));
    }

    @Override
    public <R> Mono<R> method(String name, Entity<?> entity, GenericType<R> genericType) {
        return mono(webClient.async().method(name, entity, genericType));
    }

    private <R> Mono<R> mono(Future<R> future) {
        Mono<R> mono = Mono.fromFuture(toCompletableFuture(future));
        if (scheduler != null) {
            mono = mono.subscribeOn(scheduler);
        }
        return mono;
    }
}
