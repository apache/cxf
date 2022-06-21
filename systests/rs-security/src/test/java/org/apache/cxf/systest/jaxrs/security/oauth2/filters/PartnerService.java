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

package org.apache.cxf.systest.jaxrs.security.oauth2.filters;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.filters.OAuth2FiltersTest.BookServerOAuth2Filters;

import static org.apache.cxf.rs.security.oauth2.utils.OAuthConstants.BEARER_AUTHORIZATION_SCHEME;

/**
 * A "Partner" service that delegates an "echoBook" call to the BookStore, first getting an OAuth token using the
 * ClientCodeRequestFilter.
 */
@Path("/bookstore")
public class PartnerService {

    @Context
    private ClientTokenContext context;

    @POST
    @Path("/books")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Book echoBookXml(Book book) {

        String address = "https://localhost:" + BookServerOAuth2Filters.PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address)
            .type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML)
            .authorization(new ClientAccessToken(BEARER_AUTHORIZATION_SCHEME, context.getToken().getTokenKey()));

        // Now make a service invocation with the access token
        return client.post(book, Book.class);
    }

}
