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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.stax.FOMEntry;
import org.apache.abdera.parser.stax.FOMFeed;
import org.apache.cxf.systests.cdi.base.Book;
import org.apache.cxf.systests.cdi.base.BookStoreService;

@Path("/bookstore/")
public class BookStoreFeed {
    private final BookStoreService service;

    @QueryParam("fieldValue")
    private String fieldValue;

    public BookStoreFeed(BookStoreService service) {
        this.service = service;
    }

    @GET
    @Path("/books/feed")
    @NotNull @Valid
    @Produces("application/atom+xml")
    public Feed getBooks() {
        final FOMFeed feed = new FOMFeed();

        for (final Book book: service.all()) {
            final FOMEntry entry = new FOMEntry();
            entry.addLink("/bookstore/books/" + book.getId());
            feed.addEntry(entry);
        }

        return feed;
    }

    @GET
    @Path("/books/param")
    @Produces("text/plain")
    public Response getFieldParam() {
        return Response.ok().entity(fieldValue).build();
    }

    @GET
    @Path("/books/param2")
    @Produces("text/plain")
    public Response getFieldParam2(@QueryParam("fieldValue2") String fieldValue2) {
        return Response.ok().entity(fieldValue2).build();
    }

}
