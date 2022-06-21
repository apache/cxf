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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing: @Path("/echoxmlbookregex/{id : [5-9]{3,4}}")
 */
public class JAXRSPathRegexTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;
    public static final String PORT2 = allocatePort(JAXRSPathRegexTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServer.class, true));
        createStaticBus();
    }

    @Test
    public void testWeblientPathRegularExpression() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/echoxmlbookregex";
        WebClient client = WebClient.create(endpointAddress);
        long id = 5678;
        Book book = new Book();
        book.setId(id);

        // Successful
        Book echoedBook = client.type("application/xml").accept("application/xml")
            .path("/" + id).post(book, Book.class);
        assertEquals(id, echoedBook.getId());

        // Too long
        try {
            client.reset().type("application/xml").accept("application/xml")
                .path("/" + 56789).post(book, Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Too short
        try {
            client.reset().type("application/xml").accept("application/xml")
                .path("/" + 56).post(book, Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Wrong digits
        try {
            client.reset().type("application/xml").accept("application/xml")
                .path("/" + 1234).post(book, Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Finally try another successful call
        assertNotNull(client.reset().type("application/xml").accept("application/xml")
                      .path("/" + 8667).post(book, Book.class));
    }

    @Test
    public void testJaxrs20PathRegularExpression() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/echoxmlbookregex";
        long id = 5678;
        Book book = new Book();
        book.setId(id);

        // Successful
        Client client = ClientBuilder.newClient();
        Book echoedBook = client.target(endpointAddress).path("/" + id)
            .request("application/xml")
            .post(Entity.entity(book, "application/xml"), Book.class);
        assertEquals(id, echoedBook.getId());

        // Too long
        try {
            client.target(endpointAddress).path("/" + 56789)
                .request("application/xml")
                .post(Entity.entity(book, "application/xml"), Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Too short
        try {
            client.target(endpointAddress).path("/" + 56)
                .request("application/xml")
                .post(Entity.entity(book, "application/xml"), Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Wrong digits
        try {
            client.target(endpointAddress).path("/" + 1234)
                .request("application/xml")
                .post(Entity.entity(book, "application/xml"), Book.class);
            fail("Failure expected on a failing regex");
        } catch (NotFoundException ex) {
            // expected
        }

        // Finally try another successful call
        client = ClientBuilder.newClient();
        assertNotNull(client.target(endpointAddress).path("/" + 8667)
                      .request("application/xml")
                      .post(Entity.entity(book, "application/xml"), Book.class));
    }
}
