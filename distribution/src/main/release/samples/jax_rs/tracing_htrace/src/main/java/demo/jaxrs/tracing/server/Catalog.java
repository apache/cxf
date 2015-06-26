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

package demo.jaxrs.tracing.server;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/catalog")
public class Catalog {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private ConcurrentMap<String, String> books = new ConcurrentHashMap<>();
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public void addBook(@Suspended final AsyncResponse response, @Context final UriInfo uri, 
            @FormParam("title") final String title)  {
        executor.submit(new Runnable() {
            public void run() {
            }
        });
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getBooks() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        
        for (final Map.Entry<String, String> entry: books.entrySet()) {
            builder.add(Json.createObjectBuilder()
                .add("id", entry.getKey())
                .add("title", entry.getValue())
            );
        }
        
        return builder.build();
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getBook(@PathParam("id") final String id) {
        final String title = books.get(id);
        
        if (title == null) {
            throw new NotFoundException("Book with does not exists: " + id);
        }
        
        return Json.createObjectBuilder()
            .add("id", id)
            .add("title", title)
            .build();
    }
    
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") final String id) {
        if (books.remove(id) == null) {
            throw new NotFoundException("Book with does not exists: " + id);
        }
        
        return Response.ok().build();
    }
}


