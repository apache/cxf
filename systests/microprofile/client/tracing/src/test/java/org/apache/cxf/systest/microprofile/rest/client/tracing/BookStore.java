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
package org.apache.cxf.systest.microprofile.rest.client.tracing;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.tracing.TracerContext;

@Path("/bookstore/")
public class BookStore<T extends Closeable> {
    @Context private TracerContext tracer;

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection< Book > getBooks() throws IOException {
        try (T span = tracer.startSpan("get books")) {
            return Arrays.asList(
                new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                new Book("Mastering Apache CXF", UUID.randomUUID().toString())
            );
        }
    }

    @GET
    @Path("/book/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBook(@PathParam("id") final String id) {
        tracer.annotate("book-id", id);
        return new Book("Apache CXF in Action", id);
    }
}
