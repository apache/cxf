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

package org.apache.cxf.systest.jaxrs.description;

import java.util.Arrays;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.apache.cxf.systest.jaxrs.Book;

@Path("/bookstore") 
@Api(value = "/bookstore", description = "Sample JAX-RS service with Swagger documentation")
public class BookStoreSwagger {
    @Produces({ MediaType.APPLICATION_JSON })
    @GET
    @ApiOperation(
        value = "Get books", 
        notes = "Get books", 
        response = Book.class, 
        responseContainer = "List"
    )
    public Response getBooks(
        @ApiParam(value = "Page to fetch", required = true) @QueryParam("page") @DefaultValue("1") int page) {
        return Response.ok(
            Arrays.asList(
                new Book("Book 1", 1),
                new Book("Book 2", 2)
            )
        ).build();
    }
    
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{id}")
    @GET
    @ApiOperation(
        value = "Get book by Id", 
        notes = "Get book by Id",
        response = Book.class
    )
    public Book getBook(@ApiParam(value = "id", required = true) @PathParam("id") Long id) {
        return new Book("Book", id);
    }
    
    @Path("/{id}")
    @DELETE
    @ApiOperation(
        value = "Delete book", 
        notes = "Delete book"
    )    
    public Response delete(@ApiParam(value = "id", required = true) @PathParam("id") String id) {
        return Response.ok().build();
    }
}
