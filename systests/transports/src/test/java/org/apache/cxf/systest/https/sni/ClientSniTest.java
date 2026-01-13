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

package org.apache.cxf.systest.https.sni;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for TLS SNI
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class ClientSniTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ClientSniServer.class);

    final String clientKey;
    final Object clientVal;

    public ClientSniTest(String ck, Object value) {
        this.clientKey = ck;
        this.clientVal = value;
    }
    
    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"force.urlconnection.http.conduit", true},
            {"use.async.http.conduit", true},
            {"defaultConduit", true},
            {"org.apache.cxf.transport.http.forceVersion", "1.1"},
            {"org.apache.cxf.transport.http.forceVersion", "2"}
        });
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(ClientSniServer.class, true)
        );
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // Server directly trusts the client cert
    @org.junit.Test
    public void testSniServerNames() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientSniTest.class.getResource("client-sni.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        ((BindingProvider)port).getRequestContext().put(clientKey, clientVal);

        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
