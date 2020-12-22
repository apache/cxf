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

package org.apache.cxf.systest.jaxrs;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@Path("/bookstore2")
public class BookStorePerRequest {

    private HttpHeaders httpHeaders;
    private Map<Long, Book> books = new HashMap<>();
    private List<String> bookIds;
    private List<String> setterBookIds;

    public BookStorePerRequest() {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers) {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers, Long bar) {
        throw new RuntimeException();
    }

    public BookStorePerRequest(@Context HttpHeaders headers,
                               @HeaderParam("BOOK") List<String> bookIds) {
        if (!bookIds.contains("3")) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Constructor: Header value 3 is required").build());
        }
        httpHeaders = headers;
        this.bookIds = bookIds;
        init();
    }

    @HeaderParam("Book")
    public void setBook(List<String> ids) {
        if (!ids.equals(bookIds) || ids.size() != 3) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Param setter: 3 header values are required").build());
        }
        setterBookIds = ids;
    }

    @Context
    public void setHttpHeaders(HttpHeaders headers) {
        List<String> ids = httpHeaders.getRequestHeader("BOOK");
        if (ids.contains("4")) {
            throw new ClientErrorException(Response.status(400).type("text/plain")
                                           .entity("Context setter: unexpected header value").build());
        }
    }

    @GET
    @Path("/book%20headers/")
    public Book getBookByHeader2() throws Exception {
        return getBookByHeader();
    }

    @GET
    @Path("/bookheaders/")
    public Book getBookByHeader() throws Exception {

        List<String> ids = httpHeaders.getRequestHeader("BOOK");
        if (!ids.equals(bookIds)) {
            throw new RuntimeException();
        }
        return doGetBook(ids.get(0) + ids.get(1) + ids.get(2));
    }

    @GET
    @Path("/bookheaders/injected")
    public Book getBookByHeaderInjected() throws Exception {

        return doGetBook(setterBookIds.get(0) + setterBookIds.get(1) + setterBookIds.get(2));
    }

    private Book doGetBook(String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(Long.parseLong(id));
        throw new BookNotFoundFault(details);
    }


    final void init() {
        Book book = new Book();
        book.setId(123);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }

}


