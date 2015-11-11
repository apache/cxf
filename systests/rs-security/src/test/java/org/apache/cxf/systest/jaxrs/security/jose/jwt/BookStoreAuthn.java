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

package org.apache.cxf.systest.jaxrs.security.jose.jwt;


import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.junit.Assert;

@Path("/bookstore")
public class BookStoreAuthn {
    
    @Context 
    MessageContext jaxrsContext;
    
    public BookStoreAuthn() {
    }
    
    @POST
    @Path("/books")
    @Produces("text/plain")
    @Consumes("text/plain")
    public String echoText(String text) {
        checkAuthentication();
        return text;
    }
    
    @POST
    @Path("/books")
    @Produces("application/json")
    @Consumes("application/json")
    public Book echoBook(Book book) {
        checkAuthentication();
        return book;
    }
    
    @POST
    @Path("/books")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book echoBook2(Book book) {
        checkAuthentication();
        return book;
    }
    
    private void checkAuthentication() {
        // Check that we have an authenticated principal
        Assert.assertNotNull(jaxrsContext.getSecurityContext().getUserPrincipal());
    }
}


