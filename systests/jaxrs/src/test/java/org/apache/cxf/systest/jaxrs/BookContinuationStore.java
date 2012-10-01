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

package org.apache.cxf.systest.jaxrs;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.TimeoutHandler;

@Path("/bookstore")
public class BookContinuationStore {

    private Map<String, String> books = new HashMap<String, String>();
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                        new ArrayBlockingQueue<Runnable>(10));
    
    public BookContinuationStore() {
        init();
    }
    
    @GET
    @Path("/books/defaulttimeout")
    public void getBookDescriptionWithHandler(AsyncResponse async) {
        async.register(new CompletionCallbackImpl());
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
    }
    
    @GET
    @Path("/books/timeouthandler/{id}")
    public void getBookDescriptionWithHandler(@PathParam("id") String id, AsyncResponse async) {
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new TimeoutHandlerImpl(id));
    }
    
    @GET
    @Path("/books/{id}")
    public void getBookDescription(@PathParam("id") String id, AsyncResponse async) {
        handleContinuationRequest(id, async);
    }
    
    @Path("/books/subresources/")
    public BookContinuationStore getBookStore() {
        
        return this;
        
    }
    
    @GET
    @Path("{id}")
    public void handleContinuationRequest(@PathParam("id") String id, AsyncResponse response) {
        resumeSuspended(id, response);
    }
    
    
    
    private void resumeSuspended(final String id, final AsyncResponse response) {
        
        executor.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }       
                response.resume(books.get(id));
            }
        });
        
    }
    
    private void init() {
        books.put("1", "CXF in Action1");
        books.put("2", "CXF in Action2");
        books.put("3", "CXF in Action3");
        books.put("4", "CXF in Action4");
        books.put("5", "CXF in Action5");
    }
     
    private class TimeoutHandlerImpl implements TimeoutHandler {

        private String id;
        
        public TimeoutHandlerImpl(String id) {
            this.id = id;
        }
        
        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            asyncResponse.resume(books.get(id));
        }
        
    }
    
    private class CompletionCallbackImpl implements CompletionCallback {

        @Override
        public void onComplete() {
            System.out.println("CompletionCallbackImpl: onComplete");
            
        }

        @Override
        public void onError(Throwable throwable) {
            // TODO Auto-generated method stub
            
        }
        
    }
}


