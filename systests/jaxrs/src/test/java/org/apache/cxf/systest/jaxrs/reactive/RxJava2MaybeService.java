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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.reactivestreams.server.AbstractSubscriber;
import org.apache.cxf.systest.jaxrs.reactor.HelloWorldBean;

import io.reactivex.Maybe;

@Path("/rx2/maybe")
public class RxJava2MaybeService {

    @GET
    @Produces("application/json")
    @Path("textJson")
    public Maybe<HelloWorldBean> getJson() {
        return Maybe.just(new HelloWorldBean());
    }

    @GET
    @Produces("text/plain")
    @Path("textAsync")
    public void getTextAsync(@Suspended final AsyncResponse ar) {
        final StringAsyncSubscriber subscriber = new StringAsyncSubscriber(ar);
        
        Maybe
            .just("Hello, ")
            .map(s -> s + "world!")
            .subscribe(
                s -> {
                    subscriber.onNext(s);
                    subscriber.onComplete();
                },
                subscriber::onError);

    }
    
    @GET
    @Produces("application/json")
    @Path("error")
    public Maybe<HelloWorldBean> getError() {
        return Maybe.error(new RuntimeException("Oops"));
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("empty")
    public Maybe<HelloWorldBean> empty() { 
        return Maybe.empty(); 
    }

    private static class StringAsyncSubscriber extends AbstractSubscriber<String> {
        StringAsyncSubscriber(AsyncResponse ar) {
            super(ar);
        }
    }
}