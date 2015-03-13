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

package org.apache.cxf.systest.https.ciphersuites;

import java.net.URL;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A set of tests for TLS ciphersuites
 */
public class CipherSuitesTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(CipherSuitesServer.class);
    static final String PORT2 = allocatePort(CipherSuitesServer.class, 2);
    static final String PORT3 = allocatePort(CipherSuitesServer.class, 3);
    static final String PORT4 = allocatePort(CipherSuitesServer.class, 4);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(CipherSuitesServer.class, true)
        );
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // Both client + server include AES
    @org.junit.Test
    public void testAESIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

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
    
    // Both client + server include AES
    @org.junit.Test
    public void testAESIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Both client + server include a specific AES CipherSuite (not via a filter)
    @org.junit.Test
    public void testAESIncludedExplicitly() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-explicit-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT4);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client only includes RC4, server only includes AES
    @org.junit.Test
    public void testClientRC4ServerAESIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-rc4-client.xml");

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
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client only includes RC4, server only includes AES
    @org.junit.Test
    public void testClientRC4ServerAESIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-rc4-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Both client + server include RC4
    @org.junit.Test
    public void testRC4Included() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-rc4-client.xml");

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
    
    // Both client + server include RC4
    @org.junit.Test
    public void testRC4IncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-rc4-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT2);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client only includes AES, server only includes RC4
    @org.junit.Test
    public void testClientAESServerRC4Included() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

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
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client only includes AES, server only includes RC4
    @org.junit.Test
    public void testClientAESServerRC4IncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT2);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Both client + server include NULL
    @org.junit.Test
    public void testNULLIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-null-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT3);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Both client + server include NULL
    @org.junit.Test
    public void testNULLIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-null-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT3);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client does not allow NULL
    @org.junit.Test
    public void testClientAESServerNULL() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT3);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Client does not allow NULL
    @org.junit.Test
    public void testClientAESServerNULLAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT3);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client enables a TLS v1.2 CipherSuite
    @org.junit.Test
    public void testAESIncludedTLSv12() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-tlsv12.xml");

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
}
