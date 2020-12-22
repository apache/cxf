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
package org.apache.cxf.systest.jaxrs.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/bookstore/")
public class BookStoreWithValidation extends AbstractBookStoreWithValidation implements  BookStoreValidatable {
    private Map< String, BookWithValidation > books = new HashMap<>();

    public BookStoreWithValidation() {
    }

    @GET
    @Path("/books/{bookId}")
    @Override
    @NotNull
    public BookWithValidation getBook(@PathParam("bookId") String id) {
        return books.get(id);
    }

    @GET
    @Path("/booksResponse/{bookId}")
    @Valid @NotNull
    public Response getBookResponse(@PathParam("bookId") String id) {
        return Response.ok(books.get(id)).build();
    }

    @GET
    @Path("/booksResponseNoValidation/{bookId}")
    public Response getBookResponseNoValidation(@PathParam("bookId") String id) {
        return Response.ok(books.get(id)).build();
    }

    @Path("/sub")
    public BookStoreWithValidation getBookResponseSub() {
        return this;
    }

    @POST
    @Path("/books")
    public Response addBook(@Context final UriInfo uriInfo,
            @NotNull @Size(min = 1, max = 50) @FormParam("id") String id,
            @FormParam("name") String name) {
        books.put(id, new BookWithValidation(name, id));
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).build();
    }
    
    @POST
    @Path("/booksNoValidate")
    @ValidateOnExecution(type = ExecutableType.NONE)
    public Response addBookNoValidation(@NotNull @FormParam("id") String id) {
        return Response.ok().build();
    }
    @POST
    @Path("/booksValidate")
    @ValidateOnExecution(type = ExecutableType.IMPLICIT)
    public Response addBookValidate(@NotNull @FormParam("id") String id) {
        return Response.ok().build();
    }

    @POST
    @Path("/books/direct")
    @Consumes("text/xml")
    public Response addBookDirect(@Valid BookWithValidation book, @Context final UriInfo uriInfo) {
        books.put(book.getId(), book);
        return Response.created(uriInfo.getRequestUriBuilder().path(book.getId()).build()).build();
    }

    @POST
    @Path("/books/directmany")
    @Consumes("text/xml")
    public Response addBooksDirect(@Valid List<BookWithValidation> list, @Context final UriInfo uriInfo) {
        books.put(list.get(0).getId(), list.get(0));
        return Response.created(uriInfo.getRequestUriBuilder().path(list.get(0).getId()).build()).build();
    }

    @GET
    @Path("/books")
    @Override
    public Collection< BookWithValidation > list(@DefaultValue("1") @QueryParam("page") int page) {
        return books.values();
    }

    @DELETE
    @Path("/books")
    public Response clear() {
        books.clear();
        return Response.ok().build();
    }
}
