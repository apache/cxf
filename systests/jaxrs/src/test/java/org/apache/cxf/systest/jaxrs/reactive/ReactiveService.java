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


import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.apache.cxf.jaxrs.rx.server.AbstractAsyncSubscriber;
import org.apache.cxf.jaxrs.rx.server.JsonStreamingAsyncSubscriber;
import org.apache.cxf.jaxrs.rx.server.ListAsyncSubscriber;

import rx.Observable;
import rx.schedulers.Schedulers;


@Path("/reactive")
public class ReactiveService {
    
    @GET
    @Produces("text/plain")
    @Path("text")
    public Observable<String> getText() {
        return Observable.just("Hello, world!");
    }
    
    @GET
    @Produces("text/plain")
    @Path("textAsync")
    public void getTextAsync(@Suspended final AsyncResponse ar) {
        Observable.just("Hello, ").map(s -> s + "world!")
            .subscribe(new StringAsyncSubscriber(ar));
        
    }
    
    @GET
    @Produces("application/json")
    @Path("textJson")
    public Observable<HelloWorldBean> getJson() {
        return Observable.just(new HelloWorldBean());
    }
    
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitList")
    public Observable<HelloWorldBean> getJsonImplicitList() {
        HelloWorldBean bean1 = new HelloWorldBean();
        HelloWorldBean bean2 = new HelloWorldBean("Ciao");
        return Observable.just(bean1, bean2);
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
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }  
                Observable.just(bean1, bean2).subscribe(new ListAsyncSubscriber<HelloWorldBean>(ar));
            }
        }).start();
        
    }
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitListAsyncStream")
    public void getJsonImplicitListStreamingAsync(@Suspended AsyncResponse ar) {
        Observable.just("Hello", "Ciao")
            .map(s -> new HelloWorldBean(s))
            .subscribeOn(Schedulers.computation())
            .subscribe(new JsonStreamingAsyncSubscriber<HelloWorldBean>(ar));
    }
    @GET
    @Produces("application/json")
    @Path("textJsonList")
    public Observable<List<HelloWorldBean>> getJsonList() {
        HelloWorldBean bean1 = new HelloWorldBean();
        HelloWorldBean bean2 = new HelloWorldBean();
        bean2.setGreeting("Ciao");
        return Observable.just(Arrays.asList(bean1, bean2));
    }
    
    private class StringAsyncSubscriber extends AbstractAsyncSubscriber<String> {
        
        private StringBuilder sb = new StringBuilder();
        StringAsyncSubscriber(AsyncResponse ar) {
            super(ar);
        }
        @Override
        public void onCompleted() {
            super.resume(sb.toString());
        }

        @Override
        public void onNext(String s) {
            sb.append(s);
        }
        
    }
}


