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
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class BookStoreDispatch {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Long mainId = 123L;
    
    public BookStoreDispatch() {
        init();
    }
    
    @GET
    @Path("/books/html/{bookid}")
    @Produces("text/html")
    public Book getBookHtml() {
        return books.get(123L);
    }
    
    final void init() {
        Book book = new Book();
        book.setId(mainId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
}


