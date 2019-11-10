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

package org.apache.cxf.systest.jaxrs.resources;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.Parameter;

@Component
@Path("library")
public class Library {
    private Map<String, Book> books = Collections.synchronizedMap(
        new TreeMap<String, Book>(String.CASE_INSENSITIVE_ORDER));

    public Library() {
        books.put("1", new Book("Book #1", "John Smith"));
        books.put("2", new Book("Book #2", "Tom Tommyknocker"));
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @GET
    public Response getBooks(@Parameter(required = true) @QueryParam("page") @DefaultValue("1") int page) {
        return Response.ok(books.values()).build();
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @Path("{id}")
    @GET
    public Response getBook(@PathParam("id") String id) {
        return books.containsKey(id) 
            ? Response.ok().entity(books.get(id)).build() 
                : Response.status(Status.NOT_FOUND).build();
    }
}
