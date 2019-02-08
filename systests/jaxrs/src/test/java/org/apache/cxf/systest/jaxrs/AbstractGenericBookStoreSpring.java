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


import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;

@Path("/")
@Consumes({"application/json", "application/xml" })
@Produces({"application/json", "application/xml" })
public abstract class AbstractGenericBookStoreSpring<T extends SuperBookInterface> {

    @POST
    @Path("/books/superbook")
    public T echoSuperBookJson(T book) {
        if (((SuperBook)book).isSuperBook()) {
            return book;
        }
        throw new WebApplicationException(400);
    }

    @POST
    @Path("/books/superbooks")
    public List<T> echoSuperBookCollectionJson(List<T> book) {
        if (((SuperBook)book.get(0)).isSuperBook()) {
            return book;
        }
        throw new WebApplicationException(400);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("/books/superbooks2")
    public GenericEntity<List<T>> getSuperBookCollectionGenericEntity() {
        return new GenericEntity<List<T>>((List<T>)Collections.singletonList(
            new SuperBook("Super", 124L, true))) {
        };
    }

}


