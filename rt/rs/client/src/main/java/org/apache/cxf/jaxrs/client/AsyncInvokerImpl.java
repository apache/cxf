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

import java.util.concurrent.Future;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class AsyncInvokerImpl implements AsyncInvoker {

    private WebClient wc;

    public AsyncInvokerImpl(WebClient wc) {
        this.wc = wc;
    }
    @Override
    public Future<Response> get() {
        return get(Response.class);
    }

    @Override
    public <T> Future<T> get(Class<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <T> Future<T> get(GenericType<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <T> Future<T> get(InvocationCallback<T> callback) {
        return method(HttpMethod.GET, callback);
    }

    @Override
    public Future<Response> put(Entity<?> entity) {
        return put(entity, Response.class);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
        return method(HttpMethod.PUT, entity, callback);
    }

    @Override
    public Future<Response> post(Entity<?> entity) {
        return post(entity, Response.class);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
        return method(HttpMethod.POST, entity, callback);
    }

    @Override
    public Future<Response> delete() {
        return delete(Response.class);
    }

    @Override
    public <T> Future<T> delete(Class<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <T> Future<T> delete(GenericType<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <T> Future<T> delete(InvocationCallback<T> callback) {
        return method(HttpMethod.DELETE, callback);
    }

    @Override
    public Future<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public Future<Response> head(InvocationCallback<Response> callback) {
        return method(HttpMethod.HEAD, callback);
    }

    @Override
    public Future<Response> options() {
        return options(Response.class);
    }

    @Override
    public <T> Future<T> options(Class<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <T> Future<T> options(GenericType<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <T> Future<T> options(InvocationCallback<T> callback) {
        return method(HttpMethod.OPTIONS, callback);
    }

    @Override
    public Future<Response> trace() {
        return trace(Response.class);
    }

    @Override
    public <T> Future<T> trace(Class<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> Future<T> trace(GenericType<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> Future<T> trace(InvocationCallback<T> callback) {
        return method("TRACE", callback);
    }

    @Override
    public Future<Response> method(String name) {
        return method(name, Response.class);
    }

    @Override
    public <T> Future<T> method(String name, Class<T> responseType) {
        return wc.doInvokeAsync(name, null, null, null, responseType, responseType, null);
    }

    @Override
    public <T> Future<T> method(String name, GenericType<T> responseType) {
        return wc.doInvokeAsync(name, null, null, null, responseType.getRawType(),
                             responseType.getType(), null);
    }

    @Override
    public <T> Future<T> method(String name, InvocationCallback<T> callback) {
        return wc.doInvokeAsyncCallback(name, null, null, null, callback);
    }

    @Override
    public Future<Response> method(String name, Entity<?> entity) {
        return method(name, entity, Response.class);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return wc.doInvokeAsync(name,
                             entity,
                             null,
                             null, responseType, responseType, null);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return wc.doInvokeAsync(name,
                             entity,
                             null,
                             null, responseType.getRawType(), responseType.getType(), null);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
        return wc.doInvokeAsyncCallback(name,
                                     entity,
                                     null,
                                     null,
                                     callback);
    }

}
