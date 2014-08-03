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
package org.apache.cxf.systest.jaxrs.cdi;

import java.util.Collection;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.systest.jaxrs.BookStoreService;

@Path("/bookstore/")
public class BookStore {
    @Inject private BookStoreService service;
    @Inject private String version;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() {
        return version;    
    }
    
    @GET
    @Path("/books/{bookId}")
    @NotNull
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBook(@PathParam("bookId") String id) {
        return service.get(id);
    }
    
    @GET
    @Path("/books")
    @NotNull @Valid
    @Produces(MediaType.APPLICATION_JSON)
    public Collection< Book > getBooks() {
        return service.all();
    }
    
    @POST
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBook(@Context final UriInfo uriInfo, 
            @NotNull @Size(min = 1, max = 50) @FormParam("id") String id,
            @NotNull @FormParam("name") String name) {
        final Book book = service.store(id, name);   
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).entity(book).build();
    }
}
