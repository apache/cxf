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
package org.apache.cxf.systest.microprofile.rest.client.regex;

import java.net.URI;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing: @Path("/echoxmlbookregex/{id : [5-9]{3,4}}")
 */
public class JaxrsPathRegexTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JaxrsPathRegexTest.class);

    WebClient client;
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class,
                new SingletonResourceProvider(new BookStore()));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setPublishedEndpointUrl("/");
            sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {

        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
        System.out.println("Listening on port " + PORT);
    }

    @Test
    public void testPathRegularExpression() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/";
        long id = 5678;
        Book book = new Book();
        book.setId(id);

        // Successful
        BookStoreClient bookStoreClient = RestClientBuilder.newBuilder()
            .baseUri(URI.create(endpointAddress))
            .build(BookStoreClient.class);

        Book echoedBook = bookStoreClient.echoXmlBookregex(book, String.valueOf(id));
        assertEquals(id, echoedBook.getId());

        // Too long
        try {
            bookStoreClient.echoXmlBookregex(book, "56789");
            fail("Failure expected on a failing regex");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // Too short
        try {
            bookStoreClient.echoXmlBookregex(book, "56");
            fail("Failure expected on a failing regex");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // Wrong digits
        try {
            bookStoreClient.echoXmlBookregex(book, "1234");
            fail("Failure expected on a failing regex");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // Finally try another successful call
        echoedBook = bookStoreClient.echoXmlBookregex(book, "8667");
        assertEquals(id, echoedBook.getId());
    }

}
