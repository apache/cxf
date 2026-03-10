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


import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Path("/bookstore")
public class BookCxfContinuationStore {

    private Map<String, String> books = new HashMap<>();
    private Map<String, Continuation> suspended = new ConcurrentHashMap<>();
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                        new ArrayBlockingQueue<Runnable>(10));

    @Context
    private MessageContext context;

    public BookCxfContinuationStore() {
        init();
    }

    @GET
    @Path("/books/{id}")
    public String getBookDescription(@PathParam("id") String id) {
        URI uri = context.getUriInfo().getAbsolutePath();
        if (!uri.toString().contains("/books/")) {
            throw new WebApplicationException(500);
        }
        return handleContinuationRequest(id);

    }

    @Path("/books/subresources/")
    public BookCxfContinuationStore getBookStore() {
        return this;
    }

    @GET
    @Path("{id}")
    public String handleContinuationRequest(@PathParam("id") String id) {
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
        Continuation suspendedCont = suspended.get(name);
        if (suspendedCont != null) {
            synchronized (suspendedCont) {
                suspendedCont.resume();
            }
        }
    }

    private void suspendInvocation(final String name, Continuation cont) {

        //System.out.println("Suspending invocation for " + name);

        try {
            cont.suspend(500000);
        } finally {
            suspended.put(name, cont);
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

        ContinuationProvider provider =
            (ContinuationProvider)context.get(ContinuationProvider.class.getName());

        if (provider == null) {
            Message m = PhaseInterceptorChain.getCurrentMessage();
            UriInfo uriInfo = new UriInfoImpl(m);
            if (uriInfo.getAbsolutePath().toString().contains("/books/subresources/")) {
                // when we suspend a CXF continuation from a sub-resource, the invocation will
                // return directly to that object - and sub-resources do not have contexts supported
                // by default - so we just need to depend on PhaseInterceptorChain
                provider = (ContinuationProvider)m.get(ContinuationProvider.class.getName());
            }
        }
        if (provider == null) {
            throw new WebApplicationException(500);
        }

        synchronized (suspended) {
            Continuation suspendedCont = suspended.remove(name);
            if (suspendedCont != null) {
                return suspendedCont;
            }
        }

        return provider.getContinuation();
    }

    private void init() {
        books.put("1", "CXF in Action1");
        books.put("2", "CXF in Action2");
        books.put("3", "CXF in Action3");
        books.put("4", "CXF in Action4");
        books.put("5", "CXF in Action5");
        books.put("A B C", "CXF in Action A B C");
    }

}


