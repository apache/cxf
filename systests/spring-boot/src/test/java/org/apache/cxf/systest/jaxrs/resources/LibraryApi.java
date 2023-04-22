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

package org.apache.cxf.systest.jaxrs.resources;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.TRACE;

public interface LibraryApi {
    @Produces({ MediaType.APPLICATION_JSON })
    @GET
    Response getBooks(@QueryParam("page") @DefaultValue("1") int page);

    @Produces({ MediaType.APPLICATION_JSON })
    @Path("{id}")
    @GET
    Response getBook(@PathParam("id") String id);
    
    @DELETE
    void deleteBooks();
    
    @Path("/catalog")
    Catalog catalog(); 
    
    @TRACE
    @Produces({ MediaType.APPLICATION_JSON })
    Response traceBooks();
}
