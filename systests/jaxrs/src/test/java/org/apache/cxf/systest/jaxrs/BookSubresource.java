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

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

public interface BookSubresource {

    @GET
    @Path("/subresource")
    @Produces("application/xml")
    Book getTheBook() throws BookNotFoundFault;

    @GET
    @Path("/subresource")
    @Produces("application/xml")
    Book getTheBookWithContext(@Context UriInfo ui) throws BookNotFoundFault;

    @GET
    @Path("/subresource/noproduces")
    Book getTheBookNoProduces() throws BookNotFoundFault;

    @POST
    @Path("/subresource2/{n1:.*}")
    @Consumes("text/plain")
    @Produces("application/xml")
    Book getTheBook2(@PathParam("n1") String name1,
                     @QueryParam("n2") String name2,
                     @MatrixParam("n3") String name3,
                     @MatrixParam("n33") String name33,
                     @HeaderParam("N4") String name4,
                     @CookieParam("n5") String name5,
                     String name6) throws BookNotFoundFault;

    @POST
    @Path("/subresource3")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Book getTheBook3(@FormParam("id") String id,
                     @FormParam("name") List<String> nameParts) throws BookNotFoundFault;

    @POST
    @Path("/subresource4/{id}/{name}")
    @Produces("application/xml")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Book getTheBook4(@PathParam("") Book bookPath,
                     @QueryParam("") Book bookQuery,
                     @MatrixParam("") Book matrixBook,
                     @FormParam("") Book formBook) throws BookNotFoundFault;

    @POST
    @Path("/subresource5/{id}/{name}")
    @Produces("application/xml")
    @Consumes("application/xml")
    Book getTheBook5(@PathParam("name") String name,
                     @PathParam("id") long id) throws BookNotFoundFault;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/xml")
    OrderBean addOrder(@FormParam("") OrderBean order);

    @GET
    @Path("/thebook5")
    @Produces("application/xml")
    BookBean getTheBookQueryBean(@QueryParam("") BookBean book) throws BookNotFoundFault;

}

