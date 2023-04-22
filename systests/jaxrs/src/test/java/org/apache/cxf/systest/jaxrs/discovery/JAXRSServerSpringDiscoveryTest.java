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
package org.apache.cxf.systest.jaxrs.discovery;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.apache.cxf.systest.jaxrs.validation.AbstractJAXRSValidationTest;
import org.apache.cxf.systest.jaxrs.validation.BookWithValidation;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSServerSpringDiscoveryTest extends AbstractJAXRSValidationTest {
    public static final String PORT = TestUtil.getPortNumber("jaxrs-http");

    @Ignore
    public static class Server extends AbstractSpringServer {
        public Server() {
            super("/jaxrs_spring_discovery", Integer.parseInt(PORT));
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
    }

    @Test
    public void testResponseValidationFailsIfNameIsNull()  {
        final Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1"));
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testParameterValidationFailsIfIdIsNull()  {
        final Response r = createWebClient("/bookstore/books").post(new Form().param("name", "aa"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatClientDiscoversServiceProperly() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
            "org/apache/cxf/systest/jaxrs/discovery/jaxrs-http-client.xml");
        assertEquals("http://localhost:" + PORT, WebClient.client(bs).getBaseURI().toString());

        BookWithValidation book = bs.getBook("123");
        assertEquals(book.getId(), "123");
    }

    @Override
    protected String getPort() {
        return PORT;
    }
}

