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
package org.apache.cxf.jaxrs.rx3.client;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import io.reactivex.rxjava3.core.Observable;

public interface ObservableRxInvoker extends RxInvoker<Observable<?>> {
    @Override
    Observable<Response> get();

    @Override
    <T> Observable<T> get(Class<T> responseType);

    @Override
    <T> Observable<T> get(GenericType<T> responseType);

    @Override
    Observable<Response> put(Entity<?> entity);

    @Override
    <T> Observable<T> put(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Observable<T> put(Entity<?> entity, GenericType<T> type);

    @Override
    Observable<Response> post(Entity<?> entity);

    @Override
    <T> Observable<T> post(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Observable<T> post(Entity<?> entity, GenericType<T> type);

    @Override
    Observable<Response> delete();

    @Override
    <T> Observable<T> delete(Class<T> responseType);

    @Override
    <T> Observable<T> delete(GenericType<T> responseType);

    @Override
    Observable<Response> head();

    @Override
    Observable<Response> options();

    @Override
    <T> Observable<T> options(Class<T> responseType);

    @Override
    <T> Observable<T> options(GenericType<T> responseType);

    @Override
    Observable<Response> trace();

    @Override
    <T> Observable<T> trace(Class<T> responseType);

    @Override
    <T> Observable<T> trace(GenericType<T> responseType);

    @Override
    Observable<Response> method(String name);

    @Override
    <T> Observable<T> method(String name, Class<T> responseType);

    @Override
    <T> Observable<T> method(String name, GenericType<T> responseType);

    @Override
    Observable<Response> method(String name, Entity<?> entity);

    @Override
    <T> Observable<T> method(String name, Entity<?> entity, Class<T> responseType);

    @Override
    <T> Observable<T> method(String name, Entity<?> entity, GenericType<T> responseType);
}

