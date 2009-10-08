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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookInterface;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;

@Path("/bookstorestorage/")
public class SecureBookStoreNoAnnotations implements BookInterface {
    private Map<Long, Book> books = new HashMap<Long, Book>();
  
    public SecureBookStoreNoAnnotations() {
        Book book = new Book();
        book.setId(123L);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
    public Book getThatBook(Long id) {
        return books.get(id);
    }

    public Book getThatBook(Long id, String s) {
        if (s == null) {
            throw new RuntimeException();
        }
        return books.get(id);
    }
    
    public Book getThatBook() throws BookNotFoundFault {
        return books.get(123L);
    }
}
