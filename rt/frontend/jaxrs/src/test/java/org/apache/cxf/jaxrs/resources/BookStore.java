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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/bookstore/")
public class BookStore extends BookSuperClass implements BookInterface {

    public BookStore() {
    }

    public Book getBook(String id) {
        return null;
    }

    @Override
    public Book getNewBook(String id, Boolean isNew) {
        return null;
    }



    @POST
    @Path("/books")
    public Response addBook(Book book) {
        return null;
    }

    @PUT
    @Path("/books/")
    public Response updateBook(Book book) {
        return null;
    }

    @DELETE
    @Path("/books/{bookId}/")
    public Response deleteBook(@PathParam("bookId") String id) {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    public String getAuthor() {
        return null;
    }
}


