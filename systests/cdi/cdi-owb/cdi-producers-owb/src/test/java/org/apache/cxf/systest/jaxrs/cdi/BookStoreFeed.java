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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.cxf.systests.cdi.base.AtomFeed;
import org.apache.cxf.systests.cdi.base.AtomFeedEntry;
import org.apache.cxf.systests.cdi.base.Book;
import org.apache.cxf.systests.cdi.base.BookStoreService;

@Path("/bookstore/")
public class BookStoreFeed {
    private final BookStoreService service;
    private final ServerFactoryDebugExtension debugExtension;

    public BookStoreFeed(BookStoreService service, ServerFactoryDebugExtension debugExtension) {
        this.service = service;
        this.debugExtension = debugExtension;
    }
    
    @GET
    @Path("/books/feed")
    @NotNull @Valid
    @Produces("application/atom+xml")
    public AtomFeed getBooks() {
        final AtomFeed feed = new AtomFeed();
        
        for (final Book book: service.all()) {
            final AtomFeedEntry entry = new AtomFeedEntry();
            entry.addLink("/bookstore/books/" + book.getId());
            feed.addEntry(entry);
        }
        
        return feed;
    }

    @GET
    @Path("providers")
    public String providers() {
        return debugExtension.providers();
    }
}
