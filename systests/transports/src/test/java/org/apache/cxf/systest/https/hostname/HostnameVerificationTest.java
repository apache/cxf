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

package org.apache.cxf.systest.https.hostname;

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
 * A set of tests for hostname verification, where the hostname in question is "localhost".
 * 
 * Keys created via something like:
 * keytool -genkey -validity 3650 -alias subjalt -keyalg RSA -keystore subjalt.jks 
 * -dname "CN=Colm,OU=WSS4J,O=Apache,L=Dublin,ST=Leinster,C=IE" -ext SAN=DNS:localhost
 */
public class HostnameVerificationTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(HostnameVerificationServer.class);
    static final String PORT2 = allocatePort(HostnameVerificationServer.class, 2);
    static final String PORT3 = allocatePort(HostnameVerificationServer.class, 3);
    static final String PORT4 = allocatePort(HostnameVerificationServer.class, 4);
    static final String PORT5 = allocatePort(HostnameVerificationServer.class, 5);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(HostnameVerificationServer.class, true)
        );
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // Subject Alternative Name matches (but not the CN)
    @org.junit.Test
    public void testSubjectAlternativeNameMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client.xml");

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
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Subject Alternative Name does not match (but the CN does - still an error)
    @org.junit.Test
    public void testSubjectAlternativeNameNoMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client.xml");

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
            fail("Failure expected on a non-matching subject alternative name");
        } catch (Exception ex) {
            // expected
        }
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on a non-matching subject alternative name");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // No Subject Alternative Name, but the CN matches
    @org.junit.Test
    public void testNoSubjectAlternativeNameCNMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client.xml");

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
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // No Subject Alternative Name, no matching CN
    @org.junit.Test
    public void testNoSubjectAlternativeNameNoCNMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT4);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected with no matching Subject Alt Name or CN");
        } catch (Exception ex) {
            // expected
        }
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected with no matching Subject Alt Name or CN");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
}
