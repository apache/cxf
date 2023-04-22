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
package org.apache.cxf.systests.cdi.base;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.systests.cdi.base.bindings.Logged;

@Path("/bookstore/")
public class BookStore {
    private BookStoreService service;
    private BookStoreVersion bookStoreVersion;
    private UriInfo uriInfo;
    private Injections injections;

    public BookStore() {
    }

    @Inject
    public BookStore(BookStoreService service, BookStoreVersion bookStoreVersion, UriInfo uriInfo,
                     Injections injections) {
        this.service = service;
        this.bookStoreVersion = bookStoreVersion;
        this.uriInfo = uriInfo;
        this.injections = injections;
    }

    @GET
    @Path("injections")
    public String injections() {
        return injections.state();
    }

    @Path("/version")
    public BookStoreVersion getVersion() {
        return bookStoreVersion;
    }

    @GET
    @Path("/books/{bookId}")
    @NotNull
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBook(@PathParam("bookId") String id) {
        return service.get(id);
    }

    @GET
    @Path("/books")
    @NotNull @Valid
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    public Collection< Book > getBooks() {
        return service.all();
    }

    @POST
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBook(@NotNull @Size(min = 1, max = 50) @FormParam("id") String id,
                            @NotNull @FormParam("name") String name) {
        final Book book = service.store(id, name);
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).entity(book).build();
    }
}
