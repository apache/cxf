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

package org.apache.cxf.systest.jaxrs.security;


import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/bookstore")
public class BookStore {

    public BookStore() {
    }

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) {
        return new Book();
    }

    @POST
    @Path("/books")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book addBook(Book book) {
        return book;
    }

    @POST
    @Path("/books")
    @Produces("application/xml")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Book addBookForm(@FormParam("name") String name, @FormParam("id") long id) {
        return new Book(name, id);
    }

    @POST
    @Path("/booksnoresp")
    @Produces("application/xml")
    @Consumes("application/xml")
    public void addBookNoResponse(Book book) {
    }
}


