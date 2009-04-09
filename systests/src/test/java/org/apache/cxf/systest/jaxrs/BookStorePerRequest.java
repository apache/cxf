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

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Path("/bookstore2")
public class BookStorePerRequest {

    private HttpHeaders httpHeaders;
    private Map<Long, Book> books = new HashMap<Long, Book>();
    private List<String> bookIds;
    
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
        httpHeaders = headers;
        this.bookIds = bookIds;
        init();
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
    
    private Book doGetBook(String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }        
    
    
    final void init() {
        Book book = new Book();
        book.setId(123);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
}


