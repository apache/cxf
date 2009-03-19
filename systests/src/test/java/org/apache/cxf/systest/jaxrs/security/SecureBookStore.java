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

import javax.annotation.Resource;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;

@Path("/bookstorestorage/")
public class SecureBookStore implements SecureBookInterface, Injectable {
    private Map<Long, Book> books = new HashMap<Long, Book>();
    private SecureBookInterface subresource;
    private SecurityContext securityContext; 
    
    public SecureBookStore() {
        Book book = new Book();
        book.setId(123L);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
    @Context
    public void setSecurityContext(SecurityContext sc) {
        securityContext = sc;
    }
    
    @Resource
    public void setBookStore(SecureBookInterface sb) {
        subresource = sb;
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
        if (securityContext.isUserInRole("ROLE_ADMIN")
            && !securityContext.isUserInRole("ROLE_BAZ")) {
            return books.get(123L);
        }
        throw new WebApplicationException(403);
    }

    public SecureBookInterface getBookSubResource() throws BookNotFoundFault {
        return subresource;
    }

    public Book getDefaultBook() throws BookNotFoundFault {
        return books.get(123L);
    }
}
