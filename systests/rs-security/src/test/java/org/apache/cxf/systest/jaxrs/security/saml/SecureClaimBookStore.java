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

package org.apache.cxf.systest.jaxrs.security.saml;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.cxf.security.claims.authorization.Claim;
import org.apache.cxf.security.claims.authorization.Claims;
import org.apache.cxf.systest.jaxrs.security.Book;

@Path("/bookstore")
public class SecureClaimBookStore {

    public SecureClaimBookStore() {
    }

    @POST
    @Path("/books")
    @Produces("application/xml")
    @Consumes("application/xml")
    @Claims({
        @Claim({"admin" }),
        @Claim(name = "http://claims/authentication",
               format = "http://claims/authentication-format",
               value = {"fingertip", "smartcard" })
    })
    public Book addBook(Book book) {
        return book;
    }

}


