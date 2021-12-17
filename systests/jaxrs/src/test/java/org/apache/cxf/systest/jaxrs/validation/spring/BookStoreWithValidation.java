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
package org.apache.cxf.systest.jaxrs.validation.spring;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.systest.jaxrs.validation.BookWithValidation;
import org.apache.cxf.validation.BeanValidationProvider;

@Path("/bookstore/")
public class BookStoreWithValidation {
    private BeanValidationProvider provider;

    public void setProvider(BeanValidationProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("/books")
    public Response addBook(@Context final UriInfo uriInfo,
            @NotNull @FormParam("id") String id,
            @FormParam("name") String name) {

        final BookWithValidation book = new BookWithValidation(name, id);
        provider.validateBean(book);

        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).build();
    }
}
