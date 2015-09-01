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

package org.apache.cxf.systest.jaxrs.description.group1;

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

//FIXME swagger-jaxrs 1.5.3 can't handle a self-recursive sub resource like Book. so hide Book for now
//import org.apache.cxf.systest.jaxrs.Book;

@Path("/bookstore") 
public class BookStore {
//    @Produces({ MediaType.APPLICATION_JSON })
//    @GET
//    public Response getBooks(
//        @QueryParam("page") @DefaultValue("1") int page) {
//        return Response.ok(
//            Arrays.asList(
//                new Book("Book 1", 1),
//                new Book("Book 2", 2)
//            )
//        ).build();
//    }
//    
//    @Produces({ MediaType.APPLICATION_JSON })
//    @Path("/{id}")
//    @GET
//    public Book getBook(@PathParam("id") Long id) {
//        return new Book("Book", id);
//    }

    @Produces({ MediaType.APPLICATION_JSON })
    @GET
    @Path("/names")
    public Response getBookNames(
        @QueryParam("page") @DefaultValue("1") int page) {
        return Response.ok(
            Arrays.asList(
                "Book 1",
                "Book 2"
            )
        ).build();
    }
  
    @Path("/name/{id}")
    @GET
    public String getBookName(@PathParam("id") String id) {
        return "Book of " + id;
    }

    @Path("/{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        return Response.ok().build();
    }
}
