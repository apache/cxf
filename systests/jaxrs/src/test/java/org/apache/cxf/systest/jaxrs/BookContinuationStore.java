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

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;

@Path("/bookstore")
public class BookContinuationStore {

    private Map<String, String> books = new HashMap<String, String>();
    private Map<String, Continuation> suspended = 
        new HashMap<String, Continuation>();
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                        new ArrayBlockingQueue<Runnable>(10));
    
    @Resource
    private MessageContext context;
    
    public BookContinuationStore() {
        init();
    }
    
    @GET
    @Path("/books/{id}")
    public String getBookDescription(@PathParam("id") String id) {
        
        Continuation continuation = getContinuation(id);
        if (continuation == null) {
            throw new RuntimeException("Failed to get continuation");
        }
        synchronized (continuation) {
            if (continuation.isNew()) {
                continuation.setObject(id);
                suspendInvocation(id, continuation);
            } else {
                String savedId = continuation.getObject().toString();
                if (!savedId.equals(id)) {
                    throw new RuntimeException("SavedId is wrong");
                }
                return books.get(savedId);
            }
        }
        // unreachable
        return null;
    }
    
    
    private void resumeRequest(final String name) {
        
        Continuation suspendedCont = null;
        synchronized (suspended) {
            suspendedCont = suspended.get(name);
        }
        
        if (suspendedCont != null) {
            synchronized (suspendedCont) {
                suspendedCont.resume();
            }
        }
    }
    
    private void suspendInvocation(final String name, Continuation cont) {
        
        System.out.println("Suspending invocation for " + name);
        
        try {
            cont.suspend(500000);    
        } finally {
            synchronized (suspended) {
                suspended.put(name, cont);
            }
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        // ignore
                    }       
                    resumeRequest(name);
                }
            });
        }
    }
    
    private Continuation getContinuation(String name) {
        
        System.out.println("Getting continuation for " + name);
        
        synchronized (suspended) {
            Continuation suspendedCont = suspended.remove(name);
            if (suspendedCont != null) {
                return suspendedCont;
            }
        }
        
        ContinuationProvider provider = 
            (ContinuationProvider)context.get(ContinuationProvider.class.getName());
        return provider.getContinuation();
    }
    
    private void init() {
        books.put("1", "CXF in Action1");
        books.put("2", "CXF in Action2");
        books.put("3", "CXF in Action3");
        books.put("4", "CXF in Action4");
        books.put("5", "CXF in Action5");
    }
     
}


