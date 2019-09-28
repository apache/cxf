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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSSimpleRequestDispatcherTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JAXRSSimpleRequestDispatcherTest.class);

    @Ignore
    public static class Server extends AbstractSpringServer {
        public Server() {
            super("/jaxrs_dispatch_simple", "/dispatch", Integer.parseInt(PORT));
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }



    @Test
    public void testGetTextWelcomeFile() throws Exception {
        String address = "http://localhost:" + PORT + "/dispatch/welcome.txt";
        WebClient client = WebClient.create(address);

        client.accept("text/plain");
        String welcome = client.get(String.class);
        assertEquals("Welcome", welcome);
    }

    @Test
    public void testGetRedirectedBook() throws Exception {
        String address = "http://localhost:" + PORT + "/dispatch/bookstore2/books/redirectStart";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        client.accept("application/json");
        Book book = client.get(Book.class);
        assertEquals("Redirect complete: /dispatch/bookstore/books/redirectComplete", book.getName());
    }

    @Test
    public void testGetRedirectedBook2() throws Exception {
        String address = "http://localhost:" + PORT + "/dispatch/redirect/bookstore3/books/redirectStart";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        client.accept("application/json");
        Book book = client.get(Book.class);
        assertEquals("Redirect complete: /dispatch/redirect/bookstore/books/redirectComplete",
                     book.getName());
    }

}
