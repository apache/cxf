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

package org.apache.cxf.systest.kerberos.jaxrs.kerberos;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.cxf.annotations.GZIP;

@Path("/bookstore")
@GZIP(threshold = 1)
public interface BookStore {

    @GET
    @Path("/")
    Book getBookRoot();

    @Path("/default")
    @Produces("application/xml")
    Book getDefaultBook();

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault;

    @GET
    @Path("/books/query/default")
    @Produces("application/xml")
    Book getBook(@QueryParam("bookId") long id) throws BookNotFoundFault;

    @GET
    @Path("/the books/{bookId}/")
    @Produces("application/xml")
    Book getBookWithSpace(@PathParam("bookId") String id) throws BookNotFoundFault;

    @PathParam("bookId")
    void setBookId(String id);

    void setDefaultNameAndId(String name, long id);

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/json;qs=0.9")
    Book getBookAsJSON() throws BookNotFoundFault;

    class BookNotReturnedException extends RuntimeException {

        private static final long serialVersionUID = 4935423670510083220L;

        public BookNotReturnedException(String errorMessage) {
            super(errorMessage);
        }
    }

}


