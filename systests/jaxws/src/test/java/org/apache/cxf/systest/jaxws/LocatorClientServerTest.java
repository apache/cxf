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

package org.apache.cxf.systest.jaxws;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.locator.LocatorService;
import org.apache.locator.LocatorService_Service;
import org.apache.locator.types.QueryEndpoints;
import org.apache.locator_test.LocatorServiceImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class LocatorClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(MyServer.class);

    static final Logger LOG = LogUtils.getLogger(LocatorClientServerTest.class);
    private final QName serviceName = new QName("http://apache.org/locator", "LocatorService");

    public static class MyServer extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new LocatorServiceImpl();
            String address = "http://localhost:" + PORT + "/services/LocatorService";
            Endpoint.publish(address, implementor);

        }

        public static void main(String[] args) {
            try {
                MyServer s = new MyServer();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                LOG.info("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(MyServer.class));
    }

    @Test
    @org.junit.Ignore("not legal if serviceName, portName, address all NULL")
    public void testLocatorService() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/locator.wsdl");
        assertNotNull(wsdl);

        LocatorService_Service ss = new LocatorService_Service(wsdl, serviceName);
        LocatorService port = ss.getLocatorServicePort();
        updateAddressPort(port, PORT);
        W3CEndpointReferenceBuilder builder = new  W3CEndpointReferenceBuilder();
        W3CEndpointReference w3cEpr = builder.build();
        port.registerPeerManager(w3cEpr,
                                 new Holder<W3CEndpointReference>(),
                                 new Holder<java.lang.String>());

        port.deregisterPeerManager(new java.lang.String());

        
        port.registerEndpoint(null, w3cEpr);

        
        port.deregisterEndpoint(null, w3cEpr);

        
        
        port.lookupEndpoint(new javax.xml.namespace.QName("", ""));
            
        port.listEndpoints();

        port.queryEndpoints(new QueryEndpoints());

    }

    @Test
    public void testIllegalState() throws Exception {
        W3CEndpointReferenceBuilder builder = new  W3CEndpointReferenceBuilder();
        try {
            builder.build();
            fail("Address in an EPR cannot be null, when serviceName or portName is null");
        } catch (IllegalStateException ie) {
            assertTrue(true);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getClass() + " raised: " + e.getMessage());
        }
    }
}
