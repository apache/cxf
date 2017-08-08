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

package org.apache.cxf.systest.jaxrs.reactive;


import java.util.concurrent.CompletableFuture;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.cxf.systest.jaxrs.Book;


@Path("/completable")
public class CompletableFutureService {

    @GET
    @Produces("text/xml")
    @Path("books/{id}")
    public Book getBook(@PathParam("id") long id) {
        if (123L == id) {
            return new Book("cxf", 123L);
        }
        throw new NotFoundException();
    }
    @GET
    @Produces("text/xml")
    @Path("booksAsync/{id}")
    public CompletableFuture<Book> getBookAsync(@PathParam("id") long id) {
        return CompletableFuture.supplyAsync(() -> new Book("cxf", 123L));
    }
}


