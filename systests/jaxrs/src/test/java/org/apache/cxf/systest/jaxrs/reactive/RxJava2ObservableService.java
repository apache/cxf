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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.reactivex.Observable;

@Path("/rx2/observable")
public class RxJava2ObservableService {

    @GET
    @Produces("text/plain")
    @Path("text")
    public Observable<String> getText() {
        return Observable.just("Hello, world!");
    }
    
    @GET
    @Produces("application/json")
    @Path("textJson")
    public Observable<HelloWorldBean> getJson() {
        return Observable.just(new HelloWorldBean());
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
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/empty")
    public Observable<HelloWorldBean> empty() { 
        return Observable.empty(); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/immediate/errors")
    public Observable<HelloWorldBean> immediateErrors() { 
        return Observable 
            .range(1, 2) 
            .flatMap(item -> Observable.error(new RuntimeException("Oops"))); 
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/immediate/mapper/errors")
    public Observable<HelloWorldBean> immediateMapperErrors() { 
        return Observable 
            .range(1, 2) 
            .flatMap(item -> Observable.error(new IllegalStateException("Oops"))); 
    }
}
