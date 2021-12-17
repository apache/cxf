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


import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.systest.jaxrs.Book;


@Path("/completable")
public class CompletableFutureService {

    @GET
    @Produces("text/xml")
    @Path("books/{id}")
    public Book getBook(@PathParam("id") long id) {
        if (123L == id) {
            return new Book("cxf", 123L);
        }
        throw new NotFoundException();
    }
    @GET
    @Produces("text/xml")
    @Path("booksAsync/{id}")
    public CompletableFuture<Book> getBookAsync(@PathParam("id") long id) {
        return CompletableFuture.supplyAsync(() -> new Book("cxf", 123L));
    }

    @GET
    @Produces("text/xml")
    @Path("badRequest/{id}")
    public CompletableFuture<Book> getBookAsyncExceptionBadRequest(@PathParam("id") long id) {
        return CompletableFuture.supplyAsync(() -> {
            throw new WebApplicationException(Status.BAD_REQUEST);
        });
    }

    @GET
    @Produces("text/xml")
    @Path("forbidden/{id}")
    public CompletableFuture<Book> getBookAsyncExceptionForbidden(@PathParam("id") long id) {
        final CompletableFuture<Book> future = new CompletableFuture<Book>();
        future.completeExceptionally(new WebApplicationException(Status.FORBIDDEN));
        return future;
    }

    @GET
    @Produces("text/xml")
    @Path("unauthorized/{id}")
    public void getBookAsyncExceptionUnauthorized(@PathParam("id") long id, @Suspended AsyncResponse response) {
        CompletableFuture.supplyAsync(() -> {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }).whenComplete((r, ex) -> {
            if (ex != null) {
                response.resume(ex);
            } else {
                response.resume(r);
            }
        });
    }

    @GET
    @Produces("text/xml")
    @Path("mapped/badRequest/{id}")
    public CompletableFuture<Book> getBookAsyncExceptionBadRequestMapped(@PathParam("id") long id) {
        return CompletableFuture.supplyAsync(() -> {
            throw new MappedException(Status.BAD_REQUEST);
        });
    }

    @GET
    @Produces("text/xml")
    @Path("mapped/forbidden/{id}")
    public CompletableFuture<Book> getBookAsyncExceptionForbiddenMapped(@PathParam("id") long id) {
        final CompletableFuture<Book> future = new CompletableFuture<Book>();
        future.completeExceptionally(new MappedException(Status.FORBIDDEN));
        return future;
    }

    @GET
    @Produces("text/xml")
    @Path("mapped/unauthorized/{id}")
    public void getBookAsyncExceptionUnauthorizedMapped(@PathParam("id") long id, @Suspended AsyncResponse response) {
        CompletableFuture.supplyAsync(() -> {
            throw new MappedException(Status.UNAUTHORIZED);
        }).whenComplete((r, ex) -> {
            if (ex != null) {
                response.resume(ex);
            } else {
                response.resume(r);
            }
        });
    }
}


