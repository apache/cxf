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

package org.apache.cxf.systest.jaxrs.jaxws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;
import org.apache.cxf.systest.jaxrs.BookSubresource;

@WebService
@Path("/bookstore")
@Consumes("application/xml")
@Produces("application/xml")
public interface BookStoreJaxrsJaxws {

    @WebMethod
    @GET
    @Path("/{id}")
    @Consumes("*/*")
    Book getBook(@PathParam("id") @WebParam(name = "id") Long id) throws BookNotFoundFault;

    @WebMethod
    @POST
    @Path("/books")
    Book addBook(@WebParam(name = "book") Book book);

    @Path("/books/{id}")
    BookSubresource getBookSubresource(@PathParam("id") String id);

    @Path("/thestore/{id}")
    BookStoreJaxrsJaxws getBookStore(@PathParam("id") String id);

    @POST
    @Path("/fastinfoset")
    @Consumes({"application/fastinfoset", "text/xml" })
    @Produces({"application/fastinfoset", "text/xml", "application/xml" })
    Book addFastinfoBook(Book book);

    @GET
    @Path("/fastinfoset2")
    @Produces({"application/fastinfoset", "text/xml", "application/xml" })
    Book getFastinfoBook();

    @GET
    @Path("/check/{id}")
    Response checkBook(@PathParam("id") Long id);
}
