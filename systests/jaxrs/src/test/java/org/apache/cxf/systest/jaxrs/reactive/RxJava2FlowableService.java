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

package org.apache.cxf.systest.jaxrs.reactive;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.reactivestreams.server.AbstractSubscriber;
import org.apache.cxf.jaxrs.reactivestreams.server.JsonStreamingAsyncSubscriber;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;


@Path("/flowable")
public class RxJava2FlowableService {

    
    @GET
    @Produces("application/json")
    @Path("textJson")
    public Flowable<HelloWorldBean> getJson() {
        return Flowable.just(new HelloWorldBean());
    }
    
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitListAsync")
    public void getJsonImplicitListAsync(@Suspended AsyncResponse ar) {
        final HelloWorldBean bean1 = new HelloWorldBean();
        final HelloWorldBean bean2 = new HelloWorldBean("Ciao");
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                Flowable.just(bean1, bean2).subscribe(new ListAsyncSubscriber<HelloWorldBean>(ar));
            }
        }).start();

    }
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitListAsyncStream")
    public void getJsonImplicitListStreamingAsync(@Suspended AsyncResponse ar) {
        Flowable.just("Hello", "Ciao")
            .map(HelloWorldBean::new)
            .subscribeOn(Schedulers.computation())
            .subscribe(new JsonStreamingAsyncSubscriber<HelloWorldBean>(ar));
    }
    
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitList")
    public Flowable<HelloWorldBean> getJsonImplicitList() {
        return Flowable.create(subscriber -> {
            Thread t = new Thread(() -> {
                subscriber.onNext(new HelloWorldBean("Hello"));
                sleep();
                subscriber.onNext(new HelloWorldBean("Ciao"));
                sleep();
                subscriber.onComplete();
            });
            t.start();
        }, BackpressureStrategy.MISSING);
    }
    
    @GET
    @Produces("application/json")
    @Path("textJsonSingle")
    public Single<HelloWorldBean> getJsonSingle() {
        CompletableFuture<HelloWorldBean> completableFuture = CompletableFuture
            .supplyAsync(() -> {
                sleep();
                return new HelloWorldBean("Hello");
            });
        return Single.fromFuture(completableFuture);
    }
    
    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
    
    @GET
    @Produces("text/plain")
    @Path("textAsync")
    public void getTextAsync(@Suspended final AsyncResponse ar) {
        Flowable.just("Hello, ").map(s -> s + "world!")
            .subscribe(new StringAsyncSubscriber(ar));

    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/empty")
    public Flowable<HelloWorldBean> empty() { 
        return Flowable.empty(); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/mapper/errors")
    public Flowable<HelloWorldBean> mapperErrors() { 
        return Flowable 
            .range(1, 3) 
            .flatMap(item -> { 
                if (item < 3) { 
                    return Flowable.just(new HelloWorldBean("Person " + item)); 
                } else { 
                    return Flowable.error(new IllegalArgumentException("Oops")); 
                } 
            }); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/web/errors")
    public Flowable<HelloWorldBean> webErrors() { 
        return Flowable 
            .range(1, 3) 
            .concatMap(item -> { 
                if (item < 3) { 
                    return Flowable.just(new HelloWorldBean("Person " + item)); 
                } else { 
                    return Flowable.error(new ForbiddenException("Oops")); 
                } 
            }); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/immediate/errors")
    public Flowable<HelloWorldBean> immediateErrors() { 
        return Flowable 
            .range(1, 2) 
            .flatMap(item -> Flowable.error(new RuntimeException("Oops"))); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/immediate/mapper/errors")
    public Flowable<HelloWorldBean> immediateMapperErrors() { 
        return Flowable 
            .range(1, 2) 
            .flatMap(item -> Flowable.error(new IllegalStateException("Oops"))); 
    }

    @GET
    @Path("/mixed/error")
    @Produces(MediaType.APPLICATION_JSON)
    public Flowable<HelloWorldBean> errorAndData() {
        return Flowable
            .range(1, 5)
            .flatMap(item -> {
                if (item <= 4) {
                    return Flowable.just(new HelloWorldBean(" of Item: " + item));
                } else {
                    return Flowable.error(new NotFoundException("Item not found"));
                }
            })
            .onErrorResumeNext((Throwable e) -> Flowable.error(new IllegalStateException("Oops", e)));
    }
    
    private static class StringAsyncSubscriber extends AbstractSubscriber<String> {

        private StringBuilder sb = new StringBuilder();
        StringAsyncSubscriber(AsyncResponse ar) {
            super(ar);
        }
        @Override
        public void onComplete() {
            super.resume(sb.toString());
        }

        @Override
        public void onNext(String s) {
            sb.append(s);
            super.requestNext();
        }

    }
    
    private static class ListAsyncSubscriber<T> extends AbstractSubscriber<T> {

        private List<T> beans = new LinkedList<>();
        ListAsyncSubscriber(AsyncResponse ar) {
            super(ar);
        }
        @Override
        public void onComplete() {
            super.resume(beans);
        }

        @Override
        public void onNext(T bean) {
            beans.add(bean);
            super.requestNext();
        }

    }
}


