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

import java.util.concurrent.ExecutorService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;


public class FlowableRxInvokerImpl implements FlowableRxInvoker {
    private Scheduler sc;
    private SyncInvoker syncInvoker;
    
    public FlowableRxInvokerImpl(SyncInvoker syncInvoker, ExecutorService ex) {
        this.syncInvoker = syncInvoker;
        this.sc = ex == null ? null : Schedulers.from(ex);
    }

    @Override
    public Flowable<Response> get() {
        return get(Response.class);
    }

    @Override
    public <T> Flowable<T> get(Class<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <T> Flowable<T> get(GenericType<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public Flowable<Response> put(Entity<?> entity) {
        return put(entity, Response.class);
    }

    @Override
    public <T> Flowable<T> put(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public <T> Flowable<T> put(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public Flowable<Response> post(Entity<?> entity) {
        return post(entity, Response.class);
    }

    @Override
    public <T> Flowable<T> post(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <T> Flowable<T> post(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public Flowable<Response> delete() {
        return delete(Response.class);
    }

    @Override
    public <T> Flowable<T> delete(Class<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <T> Flowable<T> delete(GenericType<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public Flowable<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public Flowable<Response> options() {
        return options(Response.class);
    }

    @Override
    public <T> Flowable<T> options(Class<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <T> Flowable<T> options(GenericType<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public Flowable<Response> trace() {
        return trace(Response.class);
    }

    @Override
    public <T> Flowable<T> trace(Class<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> Flowable<T> trace(GenericType<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public Flowable<Response> method(String name) {
        return method(name, Response.class);
    }

    @Override
    public Flowable<Response> method(String name, Entity<?> entity) {
        return method(name, entity, Response.class);
    }

    @Override
    public <T> Flowable<T> method(String name, Entity<?> entity, Class<T> responseType) {
        
        Flowable<T> flowable = Flowable.create(new FlowableOnSubscribe<T>() {
            @Override
            public void subscribe(FlowableEmitter<T> emitter) throws Exception {
                    try {
                        T response = syncInvoker.method(name, entity, responseType);
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Throwable e) {
                        emitter.onError(e);
                    }
            }
        }, BackpressureStrategy.DROP);
        
        if (sc == null) {
            return flowable;
        }
        return flowable.subscribeOn(sc).observeOn(sc);        
    }

    @Override
    public <T> Flowable<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        
        Flowable<T> flowable = Flowable.create(new FlowableOnSubscribe<T>() {
            @Override
            public void subscribe(FlowableEmitter<T> emitter) throws Exception {
                    try {
                        T response = syncInvoker.method(name, entity, responseType);
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Throwable e) {
                        emitter.onError(e);
                    }
            }
        }, BackpressureStrategy.DROP);
        
        if (sc == null) {
            return flowable; 
        }
        return flowable.subscribeOn(sc).observeOn(sc);
    }

    @Override
    public <T> Flowable<T> method(String name, Class<T> responseType) {
        
        Flowable<T> flowable = Flowable.create(new FlowableOnSubscribe<T>() {
            @Override
            public void subscribe(FlowableEmitter<T> emitter) throws Exception {
                    try {
                        T response = syncInvoker.method(name, responseType);
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Throwable e) {
                        emitter.onError(e);
                    }
            }
        }, BackpressureStrategy.DROP);
        
        if (sc == null) {
            return flowable;
        }
        return flowable.subscribeOn(sc).observeOn(sc);
    }

    @Override
    public <T> Flowable<T> method(String name, GenericType<T> responseType) {
        
        Flowable<T> flowable = Flowable.create(new FlowableOnSubscribe<T>() {
            @Override
            public void subscribe(FlowableEmitter<T> emitter) throws Exception {
                    try {
                        T response = syncInvoker.method(name, responseType);
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
            }
        }, BackpressureStrategy.DROP);
        
        if (sc == null) {
            return flowable;
        }
        return flowable.subscribeOn(sc).observeOn(sc);
    }

}
