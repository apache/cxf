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
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ConnectionCallback;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.container.TimeoutHandler;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Path("/bookstore")
public class BookContinuationStore implements BookAsyncInterface {

    private Map<String, String> books = new HashMap<>();
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                        new ArrayBlockingQueue<>(10));

    public BookContinuationStore() {
        init();
    }

    @GET
    @Path("/books/defaulttimeout")
    public void getBookDescriptionWithTimeout(@Suspended AsyncResponse async) {
        async.register(new CallbackImpl());
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
    }

    @GET
    @Path("/books/resume")
    @Produces("text/plain")
    public void getBookDescriptionImmediateResume(@Suspended AsyncResponse async) {
        async.resume("immediateResume");
    }
    @GET
    @Path("/books/resumeFromFastThread")
    @Produces("text/plain")
    public void getBookDescriptionResumeFromFastThread(@Suspended AsyncResponse async) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    // ignore
                }
                async.resume("resumeFromFastThread");
            }
        });
    }

    @GET
    @Path("/books/nocontent")
    @Produces("text/plain")
    public void getBookNoContent(AsyncResponse async) {
        async.resume(null);
    }


    public void getBookNoContentInterface(@Suspended AsyncResponse async) {
        async.resume(Response.status(206).build());
    }

    @GET
    @Path("/books/cancel")
    @SuppressWarnings("PMD.UselessPureMethodCall")
    public void getBookDescriptionWithCancel(@PathParam("id") String id,
                                             @Suspended AsyncResponse async) {
        PhaseInterceptorChain.getCurrentMessage().getClass();
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new CancelTimeoutHandlerImpl());
    }

    @GET
    @Path("/books/timeouthandler/{id}")
    public void getBookDescriptionWithHandler(@PathParam("id") String id,
                                              @Suspended AsyncResponse async) {
        async.setTimeout(1000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new TimeoutHandlerImpl(id, false));
    }

    @GET
    @Path("/books/timeouthandlerresume/{id}")
    public void getBookDescriptionWithHandlerResumeOnly(@PathParam("id") String id,
                                                        @Suspended AsyncResponse async) {
        async.setTimeout(1000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new TimeoutHandlerImpl(id, true));
    }

    @GET
    @Path("/books/{id}")
    public void getBookDescription(@PathParam("id") String id,
                                   @Suspended AsyncResponse async) {
        handleContinuationRequest(id, async);
    }

    @Path("/books/subresources/")
    public BookContinuationStore getBookStore() {

        return this;

    }

    @GET
    @Path("{id}")
    public void handleContinuationRequest(@PathParam("id") String id,
                                          @Suspended AsyncResponse response) {
        resumeSuspended(id, response);
    }

    @GET
    @Path("books/notfound")
    @Produces("text/plain")
    public void handleContinuationRequestNotFound(@Suspended AsyncResponse response) {
        response.register(new CallbackImpl());
        resumeSuspendedNotFound(response);
    }

    @GET
    @Path("books/notfound/unmapped")
    @Produces("text/plain")
    public void handleContinuationRequestNotFoundUnmapped(@Suspended AsyncResponse response) {
        response.register(new CallbackImpl());
        resumeSuspendedNotFoundUnmapped(response);
    }

    @GET
    @Path("books/notfound/unmappedImmediate")
    @Produces("text/plain")
    public void handleUnmappedImmediate(@Suspended AsyncResponse response) throws BookNotFoundFault {
        throw new BookNotFoundFault("");
    }
    @GET
    @Path("books/mappedImmediate")
    @Produces("text/plain")
    public void handleMappedImmediate(@Suspended AsyncResponse response) throws BookNotFoundFault {
        throw new WebApplicationException(Response.status(401).build());
    }

    @GET
    @Path("books/unmappedFromFilter")
    @Produces("text/plain")
    public void handleContinuationRequestUnmappedFromFilter(@Suspended AsyncResponse response) {
        response.register(new CallbackImpl());
        response.resume(Response.ok().build());
    }

    @GET
    @Path("books/suspend/unmapped")
    @Produces("text/plain")
    public void handleNotMappedAfterSuspend(@Suspended AsyncResponse response) throws BookNotFoundFault {
        response.setTimeout(2000, TimeUnit.MILLISECONDS);
        response.setTimeoutHandler(new CancelTimeoutHandlerImpl());
        throw new BookNotFoundFault("");
    }

    @GET
    @Path("/disconnect")
    public void handleClientDisconnects(@Suspended AsyncResponse response) {
        response.setTimeout(0, TimeUnit.SECONDS);

        response.register(new ConnectionCallback() {
            @Override
            public void onDisconnect(AsyncResponse disconnected) {
                System.out.println("ConnectionCallback: onDisconnect, client disconnects");
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            // ignore
        }

        response.resume(books.values().toString());
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

    private void resumeSuspendedNotFound(final AsyncResponse response) {

        executor.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                response.resume(new NotFoundException());
            }
        });

    }

    private void resumeSuspendedNotFoundUnmapped(final AsyncResponse response) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                response.resume(new BookNotFoundFault(""));
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
        private boolean resumeOnly;
        private String id;
        private AtomicInteger timeoutExtendedCounter = new AtomicInteger();

        TimeoutHandlerImpl(String id, boolean resumeOnly) {
            this.id = id;
            this.resumeOnly = resumeOnly;
        }

        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            if (!resumeOnly && timeoutExtendedCounter.addAndGet(1) <= 2) {
                asyncResponse.setTimeout(1, TimeUnit.SECONDS);
            } else {
                asyncResponse.resume(books.get(id));
            }
        }

    }

    private final class CancelTimeoutHandlerImpl implements TimeoutHandler {

        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            asyncResponse.cancel(10);

        }

    }

    private final class CallbackImpl implements CompletionCallback {

        @Override
        public void onComplete(Throwable throwable) {
            System.out.println("CompletionCallback: onComplete, throwable: " + throwable);
        }

    }


}


