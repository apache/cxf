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

package org.apache.cxf.systest.jaxrs.security.jcs;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for JWT tokens.
 */
public class JAXRSJcsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookJcsServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookJcsServer.class, true));
    }

    

    @org.junit.Test
    public void testJcsString() throws Exception {

        WebClient client = createWebClient();

        String jcs = "{}";
        String responseJcs = client.post(jcs, String.class);
        assertEquals(responseJcs, jcs);
    }

    private WebClient createWebClient() {
        URL busFile = JAXRSJcsTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();

        String address = "https://localhost:" + PORT + "/jcs/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        return client;
    }

}
