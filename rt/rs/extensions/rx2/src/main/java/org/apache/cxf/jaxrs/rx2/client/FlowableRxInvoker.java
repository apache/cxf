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
package org.apache.cxf.jaxrs.rx2.client;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import io.reactivex.Flowable;


@SuppressWarnings("rawtypes")
public interface FlowableRxInvoker extends RxInvoker<Flowable> {

    @Override
    Flowable<Response> get();

    @Override
    <T> Flowable<T> get(Class<T> responseType);

    @Override
    <T> Flowable<T> get(GenericType<T> responseType);

    @Override
    Flowable<Response> put(Entity<?> entity);

    @Override
    <T> Flowable<T> put(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Flowable<T> put(Entity<?> entity, GenericType<T> type);

    @Override
    Flowable<Response> post(Entity<?> entity);

    @Override
    <T> Flowable<T> post(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Flowable<T> post(Entity<?> entity, GenericType<T> type);

    @Override
    Flowable<Response> delete();

    @Override
    <T> Flowable<T> delete(Class<T> responseType);

    @Override
    <T> Flowable<T> delete(GenericType<T> responseType);

    @Override
    Flowable<Response> head();

    @Override
    Flowable<Response> options();

    @Override
    <T> Flowable<T> options(Class<T> responseType);

    @Override
    <T> Flowable<T> options(GenericType<T> responseType);

    @Override
    Flowable<Response> trace();

    @Override
    <T> Flowable<T> trace(Class<T> responseType);

    @Override
    <T> Flowable<T> trace(GenericType<T> responseType);

    @Override
    Flowable<Response> method(String name);

    @Override
    <T> Flowable<T> method(String name, Class<T> responseType);

    @Override
    <T> Flowable<T> method(String name, GenericType<T> responseType);

    @Override
    Flowable<Response> method(String name, Entity<?> entity);

    @Override
    <T> Flowable<T> method(String name, Entity<?> entity, Class<T> responseType);

    @Override
    <T> Flowable<T> method(String name, Entity<?> entity, GenericType<T> responseType);
}

