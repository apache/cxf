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

package org.apache.cxf.systest.jaxrs.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;
import org.springframework.security.access.annotation.Secured;

public interface SecureBookInterface {

    @GET
    @Path("/thosebooks/{bookId}/")
    @Produces("application/xml")
    @Secured({"ROLE_USER", "ROLE_ADMIN" })
    Book getThatBook(@PathParam("bookId") Long id) throws BookNotFoundFault;


    @GET
    @Path("/thosebooks/{bookId}/{id}")
    @Produces("application/xml")
    @Secured("ROLE_USER")
    Book getThatBook(@PathParam("bookId") Long id, @PathParam("id") String s) throws BookNotFoundFault;

    @GET
    @Path("/thosebooks")
    @Produces("application/xml")
    @Secured({"ROLE_ADMIN", "ROLE_BOOK_OWNER" })
    Book getThatBook() throws BookNotFoundFault;

    @Path("/subresource")
    SecureBookInterface getBookSubResource() throws BookNotFoundFault;

    @GET
    @Produces("application/xml")
    @Secured("ROLE_ADMIN")
    Book getDefaultBook() throws BookNotFoundFault;

    @Path("/securebook")
    SecureBook getSecureBook() throws BookNotFoundFault;
}
