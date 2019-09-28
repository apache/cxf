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

package org.apache.cxf.systest.jaxrs.reactor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.reactivestreams.server.JsonStreamingAsyncSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Path("/flux")
public class FluxService {

    @GET
    @Produces("application/json")
    @Path("textJson")
    public Flux<HelloWorldBean> getJson() {
        return Flux.just(new HelloWorldBean());
    }

    @GET
    @Produces("application/json")
    @Path("textJsonImplicitListAsyncStream")
    public void getJsonImplicitListStreamingAsync(@Suspended AsyncResponse ar) {
        Flux.just("Hello", "Ciao")
                .map(HelloWorldBean::new)
                .subscribeOn(Schedulers.parallel())
                .subscribe(new JsonStreamingAsyncSubscriber<>(ar));
    }
    
    @GET
    @Produces("application/json")
    @Path("textJsonImplicitListAsyncStream2")
    public Flux<HelloWorldBean> getJsonImplicitListStreamingAsync2() {
        return Flux.just("Hello", "Ciao")
                .map(HelloWorldBean::new)
                .subscribeOn(Schedulers.parallel());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("errors")
    public Flux<HelloWorldBean> errors() { 
        return Flux 
            .range(1, 2) 
            .flatMap(item -> { 
                if (item < 2) { 
                    return Mono.just(new HelloWorldBean("Person " + item)); 
                } else { 
                    return Mono.error(new RuntimeException("Oops")); 
                } 
            }); 
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/mapper/errors")
    public Flux<HelloWorldBean> mapperErrors() { 
        return Flux 
            .range(1, 3) 
            .flatMap(item -> { 
                if (item < 3) { 
                    return Mono.just(new HelloWorldBean("Person " + item)); 
                } else { 
                    return Mono.error(new IllegalArgumentException("Oops")); 
                } 
            }); 
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/immediate/errors")
    public Flux<HelloWorldBean> immediateErrors() { 
        return Flux 
            .range(1, 2) 
            .flatMap(item -> Mono.error(new RuntimeException("Oops"))); 
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/empty")
    public Flux<HelloWorldBean> empty() { 
        return Flux.empty(); 
    }
}
