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
package org.apache.cxf.systest.sts.basic_auth;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

/**
 * In this test case, a CXF JAX-RS client sends BasicAuth via (1-way) TLS to a CXF provider.
 * The provider converts it into Username Token and dispatches it to an STS for validation 
 * (via TLS).
 */
public class JaxrsBasicAuthTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);

    private static final String PORT = allocatePort(Server.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testBasicAuth() throws Exception {

        doubleIt("alice", "clarinet", false);
    }
    
    @org.junit.Test
    public void testBadBasicAuth() throws Exception {

        doubleIt("alice", "trombon", true);
    }

    private static void doubleIt(String username, String password, boolean authFailureExpected) {
        final String configLocation = "org/apache/cxf/systest/sts/basic_auth/cxf-client.xml";
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        final int numToDouble = 25;  
       
        WebClient client = WebClient.create(address, username, password, configLocation);
        client.type("text/plain").accept("text/plain");
        try {
            int resp = client.post(numToDouble, Integer.class);
            if (authFailureExpected) {
                throw new RuntimeException("Exception expected");
            }
            System.out.println("The number " + numToDouble + " doubled is " + resp);
            org.junit.Assert.assertEquals(2 * numToDouble, resp);
        } catch (ServerWebApplicationException ex) {
            if (!authFailureExpected) {
                throw new RuntimeException("Unexpected exception");
            }
            org.junit.Assert.assertEquals(500, ex.getResponse().getStatus());
        }
    }
}
