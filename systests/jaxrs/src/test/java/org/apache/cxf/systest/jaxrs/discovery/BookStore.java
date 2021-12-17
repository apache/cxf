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
package org.apache.cxf.systest.jaxrs.discovery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.cxf.systest.jaxrs.discovery.sub.BookStoreInterface;
import org.apache.cxf.systest.jaxrs.validation.BookWithValidation;

public class BookStore implements BookStoreInterface {
    @POST
    @Path("/books")
    @Valid
    public BookWithValidation addBook(@NotNull @FormParam("id") String id,
            @FormParam("name") String name) {
        return new BookWithValidation(name, id);
    }

    @GET
    @Path("/book/{id}")
    @Valid
    public BookWithValidation getBook(@NotNull @PathParam("id") String id) {
        return new BookWithValidation("", id);
    }
}
