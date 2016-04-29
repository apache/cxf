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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import rx.Observable;
import rx.Subscriber;


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
            .subscribe(new AsyncResponseSubscriber(ar));
        
    }
    
    private class AsyncResponseSubscriber extends Subscriber<String> {
        
        private StringBuilder sb = new StringBuilder();
        private AsyncResponse ar;
        
        AsyncResponseSubscriber(AsyncResponse ar) {
            this.ar = ar;
        }
        @Override
        public void onCompleted() {
            ar.resume(sb.toString());
        }

        @Override
        public void onError(Throwable arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onNext(String s) {
            sb.append(s);
        }
        
    }
}


