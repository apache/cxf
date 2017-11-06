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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import static org.apache.cxf.jaxrs.reactor.client.ReactorUtils.toCompletableFuture;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class FluxRxInvokerImpl implements FluxRxInvoker {
    private static final String TRACE = "TRACE";
    private final WebClient webClient;
    private final Scheduler scheduler;
    private final ExecutorService executorService;

    FluxRxInvokerImpl(WebClient webClient, ExecutorService executorService) {
        this.webClient = webClient;
        this.executorService = executorService;
        this.scheduler = executorService == null ? null : Schedulers.fromExecutorService(executorService);
    }

    @Override
    public Flux<Response> get() {
        return method(HttpMethod.GET);
    }

    @Override
    public <R> Flux<R> get(Class<R> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <R> Flux<R> get(GenericType<R> genericType) {
        return method(HttpMethod.GET, genericType);
    }

    @Override
    public Flux<Response> put(Entity<?> entity) {
        return method(HttpMethod.PUT, entity);
    }

    @Override
    public <R> Flux<R> put(Entity<?> entity, Class<R> responseType) {
        return method(HttpMethod.PUT, responseType);
    }

    @Override
    public <R> Flux<R> put(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.PUT, entity, genericType);
    }

    @Override
    public Flux<Response> post(Entity<?> entity) {
        return method(HttpMethod.POST, entity);
    }

    @Override
    public <R> Flux<R> post(Entity<?> entity, Class<R> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <R> Flux<R> post(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.POST, entity, genericType);
    }

    @Override
    public Flux<Response> delete() {
        return method(HttpMethod.DELETE);
    }

    @Override
    public <R> Flux<R> delete(Class<R> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <R> Flux<R> delete(GenericType<R> genericType) {
        return method(HttpMethod.DELETE, genericType);
    }

    @Override
    public Flux<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public Flux<Response> options() {
        return method(HttpMethod.OPTIONS);
    }

    @Override
    public <R> Flux<R> options(Class<R> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <R> Flux<R> options(GenericType<R> genericType) {
        return method(HttpMethod.OPTIONS, genericType);
    }

    @Override
    public Flux<Response> trace() {
        return method(TRACE);
    }

    @Override
    public <R> Flux<R> trace(Class<R> responseType) {
        return method(TRACE, responseType);
    }

    @Override
    public <R> Flux<R> trace(GenericType<R> genericType) {
        return method(TRACE, genericType);
    }

    @Override
    public Flux<Response> method(String name) {
        return method(name, Response.class);
    }

    @Override
    public <R> Flux<R> method(String name, Class<R> responseType) {
        return flux(webClient.async().method(name, responseType));
    }

    @Override
    public <R> Flux<R> method(String name, GenericType<R> genericType) {
        return flux(webClient.async().method(name, genericType));
    }

    @Override
    public Flux<Response> method(String name, Entity<?> entity) {
        return method(name, entity, Response.class);
    }

    @Override
    public <R> Flux<R> method(String name, Entity<?> entity, Class<R> responseType) {
        return flux(webClient.async().method(name, entity, responseType));
    }

    @Override
    public <R> Flux<R> method(String name, Entity<?> entity, GenericType<R> genericType) {
        return flux(webClient.async().method(name, entity, genericType));
    }

    private <R> Flux<R> flux(Future<R> future) {
        Flux<R> flux = Flux.from(Mono.fromFuture(toCompletableFuture(future, executorService)));
        if (scheduler != null) {
            flux = flux.subscribeOn(scheduler);
        }
        return flux;
    }
}
