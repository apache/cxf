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

package org.apache.cxf.systest.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/bookstore")
public class BookStoreSubObject {
    @Path("/booksubresourceobject")
    public Object getBookSubResourceObject() throws BookNotFoundFault {
        return new Book();
    }
    
    @Path("consumeslocator")
    public BookResourceLocator consumeslocator() {
        return new BookResourceLocator();
    }
    
    @GET
    @Path("{id}")
    public Book book(@PathParam("id") Long id) {
        return new Book("Book", id);
    }
}


