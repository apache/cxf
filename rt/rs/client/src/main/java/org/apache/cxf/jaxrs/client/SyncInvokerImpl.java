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

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class SyncInvokerImpl implements SyncInvoker {
    private WebClient wc;
    public SyncInvokerImpl(WebClient wc) {
        this.wc = wc;
    }

    @Override
    public Response delete() {
        return method(HttpMethod.DELETE);
    }

    @Override
    public <T> T delete(Class<T> cls) {
        return method(HttpMethod.DELETE, cls);
    }

    @Override
    public <T> T delete(GenericType<T> genericType) {
        return method(HttpMethod.DELETE, genericType);
    }

    @Override
    public Response get() {
        return method(HttpMethod.GET);
    }

    @Override
    public <T> T get(Class<T> cls) {
        return method(HttpMethod.GET, cls);
    }

    @Override
    public <T> T get(GenericType<T> genericType) {
        return method(HttpMethod.GET, genericType);
    }

    @Override
    public Response head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public Response options() {
        return method(HttpMethod.OPTIONS);
    }

    @Override
    public <T> T options(Class<T> cls) {
        return method(HttpMethod.OPTIONS, cls);
    }

    @Override
    public <T> T options(GenericType<T> genericType) {
        return method(HttpMethod.OPTIONS, genericType);
    }

    @Override
    public Response post(Entity<?> entity) {
        return method(HttpMethod.POST, entity);
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> cls) {
        return method(HttpMethod.POST, entity, cls);
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> genericType) {
        return method(HttpMethod.POST, entity, genericType);
    }

    @Override
    public Response put(Entity<?> entity) {
        return method(HttpMethod.PUT, entity);
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> cls) {
        return method(HttpMethod.PUT, entity, cls);
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> genericType) {
        return method(HttpMethod.PUT, entity, genericType);
    }

    @Override
    public Response trace() {
        return method("TRACE");
    }

    @Override
    public <T> T trace(Class<T> cls) {
        return method("TRACE", cls);
    }

    @Override
    public <T> T trace(GenericType<T> genericType) {
        return method("TRACE", genericType);
    }

    @Override
    public Response method(String method) {
        return method(method, Response.class);
    }

    @Override
    public <T> T method(String method, Class<T> cls) {
        return wc.invoke(method, null, cls);
    }

    @Override
    public <T> T method(String method, GenericType<T> genericType) {
        return wc.invoke(method, null, genericType);
    }

    @Override
    public Response method(String method, Entity<?> entity) {
        return method(method, entity, Response.class);
    }

    @Override
    public <T> T method(String method, Entity<?> entity, Class<T> cls) {
        return wc.invoke(method, entity, cls);
    }

    @Override
    public <T> T method(String method, Entity<?> entity, GenericType<T> genericType) {
        return wc.invoke(method, entity, genericType);
    }

    public WebClient getWebClient() {
        return wc;
    }

}
