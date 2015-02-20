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

package org.apache.cxf.systest.https.clientauth;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A set of tests for TLS client authentication.
 */
public class ClientAuthTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ClientAuthServer.class);
    static final String PORT2 = allocatePort(ClientAuthServer.class, 2);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(ClientAuthServer.class, true)
        );
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // Server directly trusts the client cert
    @org.junit.Test
    public void testDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Server does not (directly) trust the client cert
    @org.junit.Test
    public void testInvalidDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on an untrusted cert");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client does not specify a KeyStore, only a TrustStore
    @org.junit.Test
    public void testNoClientCert() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-no-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Server trusts the issuer of the client cert
    @org.junit.Test
    public void testChainTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-chain.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT2);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Server does not trust the issuer of the client cert
    @org.junit.Test
    public void testInvalidChainTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid2.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT2);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client does not trust the issuer of the server cert
    @org.junit.Test
    public void testClientInvalidCertChain() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid2.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client does not directly trust the server cert
    @org.junit.Test
    public void testClientInvalidDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT2);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
