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
package org.apache.cxf.systest.jaxrs.validation;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/bookstore/")
public class BookStoreWithValidationPerRequest  {
    private Map<String, BookWithValidation> books = new HashMap<>();
    @NotNull private String id;

    public BookStoreWithValidationPerRequest() {
        books.put("123", new BookWithValidation("CXF", "123"));
        books.put("124", new BookWithValidation("123"));
    }

    @QueryParam("id")
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    @GET
    @Path("book")
    @Valid @NotNull public BookWithValidation book() {
        return books.get(id);
    }

    @GET
    @Path("bookResponse")
    @Valid @NotNull public Response bookResponse() {
        return Response.ok(books.get(id)).build();
    }
}