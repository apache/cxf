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
package org.apache.cxf.systest.jaxrs.tracing;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.systest.Book;
import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;
import org.apache.htrace.core.TraceScope;

@Path("/bookstore/")
public class BookStore {
    @Context private TracerContext tracer;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
        
    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection< Book > getBooks() {
        try (final TraceScope span =  tracer.startSpan("Get Books")) {
            return Arrays.asList(
                new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                new Book("Mastering Apache CXF", UUID.randomUUID().toString())
            );
        }
    }
    
    @GET
    @Path("/books/async")
    @Produces(MediaType.APPLICATION_JSON)
    public void getBooksAsync(@Suspended final AsyncResponse response) throws Exception {
        tracer.continueSpan(new Traceable<Void>() {
            @Override
            public Void call(final TracerContext context) throws Exception {
                executor.submit(
                    tracer.wrap("Processing books", new Traceable<Void>() {
                        @Override
                        public Void call(final TracerContext context) throws Exception {
                            // Simulate some running job 
                            Thread.sleep(200);
                            
                            response.resume(
                                Arrays.asList(
                                    new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                                    new Book("Mastering Apache CXF", UUID.randomUUID().toString())
                                )
                            );
                            
                            return null;
                        }
                    }
                ));
                
                return null;
            }
        });
    }
    
    @GET
    @Path("/books/async/notrace")
    @Produces(MediaType.APPLICATION_JSON)
    public void getBooksAsyncNoTrace(@Suspended final AsyncResponse response) throws Exception {
        executor.submit(
            new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Simulate some running job 
                    Thread.sleep(200);
                    
                    response.resume(
                        Arrays.asList(
                            new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                            new Book("Mastering Apache CXF", UUID.randomUUID().toString())
                        )
                    );
                    
                    return null;
                }
            }
        );
    }
    
    @GET
    @Path("/books/pseudo-async")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Book> getBooksPseudoAsync() throws Exception {
        return tracer.continueSpan(new Traceable<Collection<Book>>() {
            @Override
            public Collection<Book> call(final TracerContext context) throws Exception {
                return tracer.wrap("Processing books", new Traceable<Collection<Book>>() {
                    @Override
                    public Collection<Book> call(final TracerContext context) throws Exception {
                        return Arrays.asList(
                            new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                            new Book("Mastering Apache CXF", UUID.randomUUID().toString())
                        );
                    }
                }).call();
            }
        });
    }
    
    @GET
    @Path("/book/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBook(@PathParam("id") final String id) {
        tracer.annotate("book-id", id);
        return new Book("Apache CXF in Action", id);
    }
    
    @PUT
    @Path("/process")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processBooks() {
        executor.submit(
            tracer.wrap("Processing books", new Traceable<Void>() {
                @Override
                public Void call(final TracerContext context) throws Exception {
                    context.timeline("Processing started");
                    return null;
                }
            })
        );
        
        return Response.ok().build();
    }
}
