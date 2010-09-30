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

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;

@Path("/bookstorestorage/")
public class SecureBookStoreNoInterface {
    private Map<Long, Book> books = new HashMap<Long, Book>();
  
    public SecureBookStoreNoInterface() {
        Book book = new Book();
        book.setId(123L);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
    @POST
    @Path("/bookforms")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN" })
    public Book getBookFromFormParams(@FormParam("name") String name, @FormParam("id") long id) {
        if (name == null || id == 0) {
            throw new RuntimeException("FormParams are not set");
        }
        return new Book(name, id);
    }
    
    @POST
    @Path("/bookforms2")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN" })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Book getBookFromHttpRequestParams(@Context HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        return getBookFromFormParams(params.get("name")[0], Long.valueOf(params.get("id")[0]));
    }
    
    @GET
    @Path("/thosebooks/{bookId}/{id}")
    @Produces("application/xml")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN" })
    public Book getThatBook(@PathParam("bookId") Long id, @PathParam("id") String s) {
        if (s == null) {
            throw new RuntimeException();
        }
        return books.get(id);
    }
    
    @GET
    @Path("/thosebooks/{bookId}/")
    @Produces("application/xml")
    @RolesAllowed("ROLE_USER")
    public Book getThatBook(@PathParam("bookId") Long id) {
        return books.get(id);
    }

    @GET
    @Path("/thosebooks")
    @Produces("application/xml")
    @RolesAllowed("ROLE_ADMIN")
    public Book getThatBook() throws BookNotFoundFault {
        return books.get(123L);
    }
    
    @Path("/securebook")
    public SecureBook getSecureBook() throws BookNotFoundFault {
        return new SecureBook("CXF in Action", 123L);
    }
}
