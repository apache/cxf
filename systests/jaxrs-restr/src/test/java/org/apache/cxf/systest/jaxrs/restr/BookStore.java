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

package org.apache.cxf.systest.jaxrs.restr;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/bookstore")
public class BookStore {

    private Map<Long, Book> books = new HashMap<>();
    private long bookId = 123;

    public BookStore() {
        init();
    }

    @GET
    @Path("/")
    public Book getBookRoot() {
        return new Book("root", 124L);
    }

    @POST
    @Path("/echoxmlbookquery")
    @Produces("application/json")
    public Book echoJsonBookQuery(@QueryParam("book") Book book, @QueryParam("id") byte id) {
        if (book.getId() != id) {
            throw new RuntimeException();
        }
        return book;
    }

    public final String init() {
        books.clear();
        bookId = 123;

        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);

        return "OK";
    }

}