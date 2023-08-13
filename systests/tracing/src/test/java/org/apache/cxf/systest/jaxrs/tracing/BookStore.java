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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.systest.Book;
import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;

@Path("/bookstore/")
public class BookStore<T extends AutoCloseable> {
    @Context private TracerContext tracer;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Book> getBooks() throws Exception {
        try (T span = tracer.startSpan("Get Books")) {
            return books();
        }
    }

    @GET
    @Path("/books/async")
    @Produces(MediaType.APPLICATION_JSON)
    public void getBooksAsync(@Suspended final AsyncResponse response) throws Exception {
        tracer.continueSpan(new Traceable<Void>() {
            @Override
            public Void call(final TracerContext context) throws Exception {
                executor.schedule(
                    tracer.wrap("Processing books", new Traceable<Void>() {
                        @Override
                        public Void call(final TracerContext context) throws Exception {
                            response.resume(books());
                            return null;
                        }
                    }
                ), 200L, TimeUnit.MILLISECONDS); // Simulate some running job
                return null;
            }
        });
    }

    @GET
    @Path("/books/async/notrace")
    @Produces(MediaType.APPLICATION_JSON)
    public void getBooksAsyncNoTrace(@Suspended final AsyncResponse response) {
        executor.schedule(() -> response.resume(books()),
                200L, TimeUnit.MILLISECONDS); // Simulate some running job
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
                        return books();
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
    public Response processBooks() throws Exception {
        tracer.wrap("Processing books", new Traceable<Void>() {
            @Override
            public Void call(final TracerContext context) throws Exception {
                context.timeline("Processing started");
                return null;
            }
        }).call();
        return Response.ok().build();
    }

    @GET
    @Path("/books/long")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Book> getBooksLong() throws InterruptedException {
        Thread.sleep(500);
        return books();
    }

    @GET
    @Path("/books/exception")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Book> getBooksException() {
        throw new InternalServerErrorException("Simulated failure");
    }

    @GET
    @Path("/books/error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooksError() {
        return Response.status(503).build();
    }

    @GET
    @Path("/books/mapper")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNpe() {
        throw new NullPointerException("Simulated failure");
    }

    private static Collection<Book> books() {
        return Arrays.asList(
                new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                new Book("Mastering Apache CXF", UUID.randomUUID().toString())
            );
    }

}
