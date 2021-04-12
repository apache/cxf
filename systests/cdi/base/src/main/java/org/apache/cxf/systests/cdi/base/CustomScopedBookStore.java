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
package org.apache.cxf.systests.cdi.base;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.systests.cdi.base.bindings.Logged;
import org.apache.cxf.systests.cdi.base.scope.CustomScoped;

@Path("/bookstore/custom")
@CustomScoped
@Logged
public class CustomScopedBookStore {
    private BookStoreService service;
    private UriInfo uriInfo;

    public CustomScopedBookStore() {
    }

    @Inject
    public CustomScopedBookStore(BookStoreService service, UriInfo uriInfo) {
        this.service = service;
        this.uriInfo = uriInfo;
    }

    @GET
    @Path("/books")
    @NotNull @Valid
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooks() {
        return Response
          .ok()
          .entity(service.all())
          .contentLocation(uriInfo.getAbsolutePath())
          .build();
    }
}
