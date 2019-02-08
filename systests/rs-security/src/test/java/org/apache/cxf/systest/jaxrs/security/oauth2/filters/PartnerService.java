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


import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.systest.jaxrs.security.Book;

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
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book echoBookXml(Book book) {

        URL busFile = PartnerService.class.getResource("client.xml");

        String address = "https://localhost:" + OAuth2FiltersTest.PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");

        client.header("Authorization", "Bearer " + context.getToken().getTokenKey());

        // Now make a service invocation with the access token
        Response serviceResponse = client.post(book);
        if (serviceResponse.getStatus() == 200) {
            return serviceResponse.readEntity(Book.class);
        }

        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

}

