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


package org.apache.cxf.jaxrs.resources;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Produces("text/plain")
@Path("/bookstore/{id}")
public class BookStoreNoSubResource {

    public BookStoreNoSubResource() {
    }

    @GET
    public String getBookStoreInfo() {
        return "This is a great store";
    }

    @GET
    @Path("/books")
    @Produces("application/xml")
    public Book getBooks() {
        return null;
    }

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) {
        return null;
    }

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/json")
    public Book getBookJSON(@PathParam("bookId") String id) {
        return null;
    }

    @POST
    @Path("/books")
    @Produces("application/xml")
    public Response addBook(Book book) {
        return null;
    }

    @PUT
    @Path("/books/")
    @Produces("application/*")
    public Response updateBook(Book book) {
        return null;
    }

    @Path("/books/{bookId}/")
    @DELETE
    @Produces("application/xml")
    public Response deleteBook(@PathParam("bookId") String id) {
        return null;
    }
}


