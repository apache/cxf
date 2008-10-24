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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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


